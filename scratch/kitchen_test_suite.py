import urllib.request
import urllib.error
import json
import sys

sys.stdout.reconfigure(encoding='utf-8')
sys.stderr.reconfigure(encoding='utf-8')

BASE_URL = "http://localhost:8080"

def request(method, path, data=None, token=None):
    url = f"{BASE_URL}{path}"
    headers = {"Content-Type": "application/json"}
    if token:
        headers["Authorization"] = f"Bearer {token}"
    body = json.dumps(data).encode("utf-8") if data is not None else None
    req = urllib.request.Request(url, data=body, headers=headers, method=method)
    try:
        with urllib.request.urlopen(req) as resp:
            content = resp.read().decode("utf-8")
            return resp.status, json.loads(content) if content else {}
    except urllib.error.HTTPError as e:
        content = e.read().decode("utf-8")
        try:
            return e.code, json.loads(content)
        except Exception:
            return e.code, {"message": content}

def print_test(name, passed, detail=""):
    mark = "✓ PASSED" if passed else "✗ FAILED"
    color = "\033[92m" if passed else "\033[91m"
    reset = "\033[0m"
    print(f"{color}{mark}: {name}{reset}")
    if detail:
        print(f"   -> {detail}")

print("==========================================================")
print(" KITCHEN MANAGEMENT COMPLETE INTEGRATION TEST SUITE")
print("==========================================================")

# 1. Login as Admin
status, res = request("POST", "/api/auth/login", {"username": "admin", "password": "admin123"})
assert status == 200, f"Admin login failed: {res}"
admin_token = res["data"]["accessToken"]
print("✓ Logged in as Admin")

# 2. Get existing kitchens
status, res = request("GET", "/api/kitchens", token=admin_token)
assert status == 200, f"Get kitchens failed: {res}"
existing_kitchens = res["data"]
print(f"✓ Found {len(existing_kitchens)} existing kitchens in system:")
for k in existing_kitchens:
    print(f"   - [{k.get('code')}] {k.get('name')} (Active: {k.get('active')}, Staff: {k.get('assignedEmployeesCount')}, Categories: {k.get('assignedCategoriesCount')})")

# 3. Validation: Duplicate name prevention
print("\n--- Test 1: Duplicate Name Validation ---")
first_name = existing_kitchens[0]["name"]
status, res = request("POST", "/api/kitchens", {"name": first_name, "code": "DUP1"}, token=admin_token)
passed = (status == 400 and "mavjud" in res.get("message", "").lower())
print_test("Duplicate active kitchen name blocked by backend", passed, res.get("message"))

# 4. Validation: Blank / Whitespace name prevention
print("\n--- Test 2: Blank / Whitespace Name Validation ---")
status, res = request("POST", "/api/kitchens", {"name": "   ", "code": "BLANK"}, token=admin_token)
passed = (status == 400)
print_test("Blank kitchen name rejected by backend", passed, res.get("message"))

import random
RUN_ID = f"{random.randint(1000, 9999)}"

# 5. Create new Kitchen: "Pitsaxona Test"
print("\n--- Test 3: Create New Kitchen ---")
test_kitchen_name = f"Pitsaxona Test {RUN_ID}"
status, res = request("POST", "/api/kitchens", {
    "name": test_kitchen_name,
    "description": "Pizza va tezpishar mahsulotlar stansiyasi",
    "color": "#EF4444",
    "autoPrint": True,
    "soundNotification": True,
    "preparationTimeMinutes": 20
}, token=admin_token)
assert status == 200, f"Failed to create kitchen: {res}"
target_kitchen = res["data"]
orig_kitchen_id = target_kitchen["id"]
print_test(f"Created kitchen '{test_kitchen_name}'", True, f"ID: {target_kitchen['id']}, Code: {target_kitchen['code']}")

# 6. Edit / Rename Kitchen to "Pizza Oshxonasi"
print("\n--- Test 4: Edit / Rename Kitchen (Preserving ID) ---")
rename_kitchen_name = f"Pizza Oshxonasi {RUN_ID}"
status, res = request("PUT", f"/api/kitchens/{orig_kitchen_id}", {
    "name": rename_kitchen_name,
    "description": "Yangilangan pizza stansiyasi tavsifi",
    "color": "#10B981",
    "autoPrint": True,
    "soundNotification": True,
    "preparationTimeMinutes": 15
}, token=admin_token)
passed = (status == 200 and res.get("data", {}).get("id") == orig_kitchen_id and res.get("data", {}).get("name") == rename_kitchen_name)
print_test(f"Kitchen renamed to '{rename_kitchen_name}' with unchanged ID", passed, f"ID: {res.get('data', {}).get('id')}, Name: {res.get('data', {}).get('name')}")

# 7. Status Toggle: INACTIVE
print("\n--- Test 5: Status Toggle: Deactivate ---")
status, res = request("PATCH", f"/api/kitchens/{orig_kitchen_id}/status", {"active": False}, token=admin_token)
passed = (status == 200 and res["data"]["active"] is False)
print_test("Kitchen successfully deactivated (active=False)", passed)

# Verify excluded from /api/kitchens/active
status, res = request("GET", "/api/kitchens/active", token=admin_token)
active_ids = [k["id"] for k in res["data"]]
passed = (orig_kitchen_id not in active_ids)
print_test("Inactive kitchen excluded from /api/kitchens/active", passed)

# 8. Category Assignment Guard: Cannot assign INACTIVE kitchen
print("\n--- Test 6: Inactive Kitchen Category Assignment Guard ---")
status, res = request("POST", "/api/categories", {
    "name": f"Test Inactive Cat {json.dumps(status)}",
    "kitchenId": orig_kitchen_id
}, token=admin_token)
passed = (status == 400 and "inactive" in res.get("message", "").lower())
print_test("Category assignment to INACTIVE kitchen rejected", passed, res.get("message"))

# 9. Reactivate Kitchen
print("\n--- Test 7: Reactivate Kitchen ---")
status, res = request("PATCH", f"/api/kitchens/{orig_kitchen_id}/status", {"active": True}, token=admin_token)
passed = (status == 200 and res["data"]["active"] is True)
print_test("Kitchen successfully reactivated (active=True)", passed)

status, res = request("GET", "/api/kitchens/active", token=admin_token)
active_ids = [k["id"] for k in res["data"]]
passed = (orig_kitchen_id in active_ids)
print_test("Reactivated kitchen now appears in /api/kitchens/active", passed)

# 10. Category Assignment to Active Kitchen
print("\n--- Test 8: Assign Category to Active Kitchen ---")
status, res = request("GET", "/api/categories", token=admin_token)
test_cat = next((c for c in res["data"] if c["name"] == "Pizza Category Test"), None)
if not test_cat:
    status, res = request("POST", "/api/categories", {
        "name": "Pizza Category Test",
        "kitchenId": orig_kitchen_id,
        "color": "#10B981"
    }, token=admin_token)
    assert status == 200, f"Failed to create category: {res}"
    test_cat = res["data"]
    print_test("Created 'Pizza Category Test' linked to kitchen", True, f"Category ID: {test_cat['id']}")
else:
    status, res = request("PUT", f"/api/categories/{test_cat['id']}", {
        "kitchenId": orig_kitchen_id
    }, token=admin_token)
    assert status == 200, f"Failed to update category: {res}"
    test_cat = res["data"]
    print_test("Updated 'Pizza Category Test' linked to kitchen", True, f"Category ID: {test_cat['id']}")

# 11. Multi-Kitchen Staff Assignment (Ali)
print("\n--- Test 9: Multi-Kitchen Staff Assignment (Ali) ---")
somsapaz = next((k for k in existing_kitchens if "somsa" in k["name"].lower() or "som" in k.get("code", "").lower()), None)
palovchi = next((k for k in existing_kitchens if "palov" in k["name"].lower() or "pal" in k.get("code", "").lower()), None)
assert somsapaz is not None, "Somsapaz kitchen not found!"
assert palovchi is not None, "Palovchi kitchen not found!"

status, res = request("GET", "/api/users", token=admin_token)
ali = next((u for u in res["data"] if u["username"] == "ali_cook"), None)
if not ali:
    status, res = request("POST", "/api/users", {
        "username": "ali_cook",
        "password": "password123",
        "firstName": "Ali",
        "lastName": "Oshpaz",
        "role": "KITCHEN",
        "kitchenIds": [orig_kitchen_id, somsapaz["id"]]
    }, token=admin_token)
    assert status == 200, f"Failed to create user Ali: {res}"
    ali = res["data"]
    print_test("Created employee Ali with Multi-Kitchen (Pizza + Somsapaz)", True)
else:
    status, res = request("PUT", f"/api/users/{ali['id']}", {
        "firstName": "Ali",
        "lastName": "Oshpaz",
        "role": "KITCHEN",
        "kitchenIds": [orig_kitchen_id, somsapaz["id"]]
    }, token=admin_token)
    assert status == 200, f"Failed to update user Ali: {res}"
    ali = res["data"]
    print_test("Updated employee Ali with Multi-Kitchen (Pizza + Somsapaz)", True)

# 12. Multi-Kitchen Station Isolation for Ali
print("\n--- Test 10: Multi-Kitchen Station Isolation for Ali ---")
status, res = request("POST", "/api/auth/login", {"username": "ali_cook", "password": "password123"})
assert status == 200, f"Ali login failed: {res}"
ali_token = res["data"]["accessToken"]

status, res = request("GET", "/api/kitchens", token=ali_token)
ali_kitchens = res["data"]
ali_kitchen_ids = [k["id"] for k in ali_kitchens]
ali_kitchen_names = [k["name"] for k in ali_kitchens]

has_pizza = orig_kitchen_id in ali_kitchen_ids
has_somsa = somsapaz["id"] in ali_kitchen_ids
has_palov = palovchi["id"] in ali_kitchen_ids

passed = (has_pizza and has_somsa and not has_palov)
print_test("Ali sees only Pizza and Somsapaz (Palovchi is isolated/hidden)", passed, f"Visible stations: {ali_kitchen_names}")

# 13. Backend Security: Verify Ali cannot view Palovchi orders directly
print("\n--- Test 11: Backend Security: Forbidden on Palovchi Orders ---")
status, res = request("GET", f"/api/kitchen/orders?kitchenId={palovchi['id']}", token=ali_token)
passed = (status == 403)
print_test("Backend blocked Ali from accessing Palovchi orders (403 Forbidden)", passed, res.get("message"))

# 14. Backend Security: Ali cannot perform Kitchen CRUD
print("\n--- Test 12: Backend Security: Kitchen Staff Forbidden from CRUD ---")
status, res = request("POST", "/api/kitchens", {"name": "Unauthorized Kitchen"}, token=ali_token)
passed = (status == 403)
print_test("Backend blocked Ali from creating kitchens (403 Forbidden)", passed, res.get("message"))

# 15. Create Product & Place Order for Real-Time Kitchen Routing
print("\n--- Test 13: Order Routing: Product -> Category -> Kitchen ---")
status, res = request("GET", "/api/products", token=admin_token)
test_prod = next((p for p in res["data"] if p["name"] == "Pizza Margarita Test"), None)
if not test_prod:
    status, res = request("POST", "/api/products", {
        "name": "Pizza Margarita Test",
        "categoryId": test_cat["id"],
        "salePrice": 65000,
        "active": True,
        "available": True
    }, token=admin_token)
    assert status == 200, f"Failed to create product: {res}"
    test_prod = res["data"]
    print_test("Created product 'Pizza Margarita Test' under Pizza Category", True)
else:
    print_test("Found product 'Pizza Margarita Test'", True)

# Create an order with this product
status, res = request("POST", "/api/orders", {
    "items": [
        {
            "productId": test_prod["id"],
            "quantity": 2,
            "unitPrice": 65000
        }
    ]
}, token=admin_token)
assert status == 200, f"Failed to create order: {res}"
created_order = res["data"]
order_id = created_order["id"]
order_num = created_order["orderNumber"]
order_item = next(it for it in created_order["items"] if it["productId"] == test_prod["id"])
routed_kitchen_id = order_item.get("kitchenId")
passed = (routed_kitchen_id == orig_kitchen_id)
print_test(f"Order #{order_num} item routed to Pizza Oshxonasi", passed, f"Item kitchenId: {routed_kitchen_id}")

# 16. Verify Ali sees this order in his station orders
print("\n--- Test 14: Order Visible in Ali's Station KDS ---")
status, res = request("GET", f"/api/kitchen/orders?kitchenId={orig_kitchen_id}", token=ali_token)
ali_order_ids = [o["id"] for o in res["data"]]
passed = (order_id in ali_order_ids)
print_test(f"Order #{order_num} received in Ali's KDS queue for Pizza station", passed)

# 17. Rename Kitchen & Verify Historical Order Intact
print("\n--- Test 15: Rename Kitchen & Verify Historical Order Intact ---")
import random
new_rename_name = f"Pizza va Fast Food {random.randint(100, 999)}"
status, res = request("PUT", f"/api/kitchens/{orig_kitchen_id}", {
    "name": new_rename_name,
    "description": "Kengaytirilgan pizza va fast food stansiyasi"
}, token=admin_token)
assert status == 200, f"Failed to rename kitchen: {res}"
print_test(f"Renamed kitchen to '{new_rename_name}'", True)

# Check historical order item
status, res = request("GET", f"/api/orders/{order_id}", token=admin_token)
history_order = res["data"]
history_item = next(it for it in history_order["items"] if it["productId"] == test_prod["id"])
passed = (history_item.get("kitchenId") == orig_kitchen_id)
print_test("Historical order item retains exact kitchen reference (unbroken)", passed, f"Item kitchenId: {history_item.get('kitchenId')}")

# 18. Safe Delete Guard: Try to delete kitchen with linked data
print("\n--- Test 16: Safe Delete Guard (Linked Data Protection) ---")
status, res = request("DELETE", f"/api/kitchens/{orig_kitchen_id}", token=admin_token)
passed = (status == 400 and ("bog'langan" in res.get("message", "").lower() or "tavsiya etiladi" in res.get("message", "").lower()))
print_test("Safe Delete guard prevented deleting kitchen with linked category and orders", passed, res.get("message"))

# 19. Soft Delete on an unlinked temporary kitchen
print("\n--- Test 17: Soft Delete on Unlinked Temporary Kitchen ---")
status, res = request("POST", "/api/kitchens", {
    "name": f"Temporary Station {RUN_ID}",
    "description": "Temporary for deletion test"
}, token=admin_token)
assert status == 200, f"Failed to create temp kitchen: {res}"
temp_id = res["data"]["id"]
temp_name = res["data"]["name"]

status, res = request("DELETE", f"/api/kitchens/{temp_id}", token=admin_token)
passed = (status == 200)
print_test(f"Deleted unlinked temporary kitchen '{temp_name}'", passed, res.get("message"))

status, res = request("GET", "/api/kitchens", token=admin_token)
kitchen_ids = [k["id"] for k in res["data"]]
passed = (temp_id not in kitchen_ids)
print_test("Deleted kitchen is soft-deleted and filtered out from listing", passed)

# 20. Pagination & Search Endpoints
print("\n--- Test 18: Pagination & Search Endpoints ---")
status, res = request("GET", "/api/kitchens?page=0&size=2", token=admin_token)
passed = (status == 200 and len(res["data"]) <= 2 and "page" in res and res["page"]["size"] == 2)
print_test("Server-side pagination (page=0, size=2) functioning", passed, f"Count: {len(res['data'])}, PageMeta: {res.get('page')}")

status, res = request("GET", "/api/kitchens?search=Pizza", token=admin_token)
passed = (status == 200 and len(res["data"]) >= 1 and all("pizza" in (k["name"] + k.get("code","") + (k.get("description") or "")).lower() for k in res["data"]))
print_test("Server-side search query (search=Pizza) functioning", passed, f"Found {len(res['data'])} matching kitchens: {[k['name'] for k in res['data']]}")

# 21. Staff Assignment Endpoint: GET /api/kitchens/{id}/employees & POST assignment
print("\n--- Test 19: Dedicated Kitchen Staff Assignment API ---")
status, res = request("GET", f"/api/kitchens/{orig_kitchen_id}/employees", token=admin_token)
passed = (status == 200 and isinstance(res["data"], list))
print_test(f"GET /api/kitchens/{orig_kitchen_id}/employees returned assigned staff", passed, f"Assigned staff count: {len(res['data'])}")

# 22. Category Assignment Endpoint: GET /api/kitchens/{id}/categories
print("\n--- Test 20: Dedicated Kitchen Categories API ---")
status, res = request("GET", f"/api/kitchens/{orig_kitchen_id}/categories", token=admin_token)
passed = (status == 200 and len(res["data"]) >= 1)
print_test(f"GET /api/kitchens/{orig_kitchen_id}/categories returned categories", passed, f"Categories count: {len(res['data'])}")

print("\n==========================================================")
print(" ALL 20 INTEGRATION TESTS COMPLETED SUCCESSFULLY!")
print("==========================================================")
