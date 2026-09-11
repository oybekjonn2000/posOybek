import { Component, OnInit, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { SettingsService, AllSettingsResponse, RestaurantSettings, GeneralSettings, ReceiptSettings, PaymentSettings, TaxServiceSettings, OrderSettings, KitchenSettings, NotificationSettings, SecuritySettings, BackupSettings, SystemInfoDto, AuditLogEntry } from '../core/services/settings.service';
import { PrinterService, Printer, CreatePrinterRequest, UpdatePrinterRequest, TestPrintResult, ExtendedKitchenStation, AvailablePrinter } from '../core/services/printer.service';
import { AuthService } from '../core/services/auth.service';

type SettingsCategory = 
  | 'RESTAURANT'
  | 'GENERAL'
  | 'USERS'
  | 'TABLES'
  | 'PRODUCTS'
  | 'KITCHENS'
  | 'PRINTERS'
  | 'RECEIPT'
  | 'PAYMENTS'
  | 'TAX_SERVICE'
  | 'ORDERS'
  | 'KITCHEN_DISPLAY'
  | 'NOTIFICATIONS'
  | 'SECURITY'
  | 'BACKUP'
  | 'SYSTEM_INFO';

@Component({
  selector: 'app-settings',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <div class="settings-container fade-in">
      <!-- HEADER -->
      <header class="settings-header">
        <div>
          <h1 class="settings-title">POS Sozlamalar Markazi</h1>
          <p class="settings-subtitle">Restoran parametrlari, oshxona bo'limlari va printerlarni boshqarish</p>
        </div>
        <div class="header-actions">
          <div class="system-status-pill" [class.online]="sysInfo()?.backendStatus?.includes('ONLINE')">
            <span class="status-dot"></span>
            <span>{{ sysInfo()?.backendStatus?.includes('ONLINE') ? 'Tizim: ONLINE' : 'Tizim: Kutilmoqda' }}</span>
          </div>
          <button class="pos-btn pos-btn-primary" (click)="saveActiveCategory()" [disabled]="saving()">
            @if (saving()) {
              <span class="spinner"></span> Saqlanmoqda...
            } @else {
              💾 Saqlash
            }
          </button>
        </div>
      </header>

      <!-- TOAST BANNER -->
      @if (toastMessage()) {
        <div class="toast-banner" [class.success]="toastType() === 'success'" [class.error]="toastType() === 'error'">
          <span>{{ toastType() === 'success' ? '✅' : '⚠️' }} {{ toastMessage() }}</span>
          <button class="toast-close" (click)="clearToast()">✕</button>
        </div>
      }

      <div class="settings-layout">
        <!-- LEFT SIDEBAR CATEGORIES -->
        <aside class="settings-nav">
          <div class="nav-group-title">RESTORAN VA UMUMIY</div>
          <button class="nav-item" [class.active]="activeCategory() === 'RESTAURANT'" (click)="setCategory('RESTAURANT')">
            <span class="nav-icon">⚙️</span>
            <span class="nav-label">Restoran profili</span>
          </button>
          <button class="nav-item" [class.active]="activeCategory() === 'GENERAL'" (click)="setCategory('GENERAL')">
            <span class="nav-icon">🌐</span>
            <span class="nav-label">Umumiy sozlamalar</span>
          </button>
          <button class="nav-item" [class.active]="activeCategory() === 'USERS'" (click)="setCategory('USERS')">
            <span class="nav-icon">👥</span>
            <span class="nav-label">Xodimlar va Rollar</span>
          </button>
          <button class="nav-item" [class.active]="activeCategory() === 'TABLES'" (click)="setCategory('TABLES')">
            <span class="nav-icon">🪑</span>
            <span class="nav-label">Stollar va Zallar</span>
          </button>
          <button class="nav-item" [class.active]="activeCategory() === 'PRODUCTS'" (click)="setCategory('PRODUCTS')">
            <span class="nav-icon">🍔</span>
            <span class="nav-label">Mahsulotlar toifalari</span>
          </button>

          <div class="nav-group-title">OSXONA VA PRINTERLAR</div>
          <button class="nav-item" [class.active]="activeCategory() === 'KITCHENS'" (click)="setCategory('KITCHENS')">
            <span class="nav-icon">👨‍🍳</span>
            <span class="nav-label">Oshxonalar / Bo'limlar</span>
            <span class="nav-badge">{{ kitchens().length }}</span>
          </button>
          <button class="nav-item highlight" [class.active]="activeCategory() === 'PRINTERS'" (click)="setCategory('PRINTERS')">
            <span class="nav-icon">🖨️</span>
            <span class="nav-label">Printer Management</span>
            <span class="nav-badge printer-badge">{{ onlinePrintersCount() }}/{{ printers().length }}</span>
          </button>
          <button class="nav-item" [class.active]="activeCategory() === 'KITCHEN_DISPLAY'" (click)="setCategory('KITCHEN_DISPLAY')">
            <span class="nav-icon">🍳</span>
            <span class="nav-label">Oshxona ekrani (KDS)</span>
          </button>

          <div class="nav-group-title">MOLIYA VA BUYURTMALAR</div>
          <button class="nav-item" [class.active]="activeCategory() === 'RECEIPT'" (click)="setCategory('RECEIPT')">
            <span class="nav-icon">🧾</span>
            <span class="nav-label">Chek va Kvitansiya</span>
          </button>
          <button class="nav-item" [class.active]="activeCategory() === 'PAYMENTS'" (click)="setCategory('PAYMENTS')">
            <span class="nav-icon">💳</span>
            <span class="nav-label">To'lov turlari</span>
          </button>
          <button class="nav-item" [class.active]="activeCategory() === 'TAX_SERVICE'" (click)="setCategory('TAX_SERVICE')">
            <span class="nav-icon">📊</span>
            <span class="nav-label">Xizmat haqi va Soliq</span>
          </button>
          <button class="nav-item" [class.active]="activeCategory() === 'ORDERS'" (click)="setCategory('ORDERS')">
            <span class="nav-icon">📋</span>
            <span class="nav-label">Buyurtma sozlamalari</span>
          </button>

          <div class="nav-group-title">TIZIM VA XAVFSIZLIK</div>
          <button class="nav-item" [class.active]="activeCategory() === 'NOTIFICATIONS'" (click)="setCategory('NOTIFICATIONS')">
            <span class="nav-icon">🔔</span>
            <span class="nav-label">Bildirishnoma & Ovoz</span>
          </button>
          <button class="nav-item" [class.active]="activeCategory() === 'SECURITY'" (click)="setCategory('SECURITY')">
            <span class="nav-icon">🔐</span>
            <span class="nav-label">Xavfsizlik & PIN</span>
          </button>
          <button class="nav-item" [class.active]="activeCategory() === 'BACKUP'" (click)="setCategory('BACKUP')">
            <span class="nav-icon">💾</span>
            <span class="nav-label">Zaxiralash & Baza</span>
          </button>
          <button class="nav-item" [class.active]="activeCategory() === 'SYSTEM_INFO'" (click)="setCategory('SYSTEM_INFO')">
            <span class="nav-icon">ℹ️</span>
            <span class="nav-label">Tizim holati & Audit</span>
          </button>
        </aside>

        <!-- RIGHT CONTENT VIEWPORT -->
        <main class="settings-content">
          @if (loading()) {
            <div class="loading-state">
              <span class="spinner-large"></span>
              <p>Sozlamalar yuklanmoqda...</p>
            </div>
          } @else {

            <!-- 1. RESTAURANT SETTINGS -->
            @if (activeCategory() === 'RESTAURANT') {
              <div class="category-card">
                <div class="card-header">
                  <h3>🏛️ Restoran Profili va Rekvizitlari</h3>
                  <p>Ushbu ma'lumotlar kassa cheklarida va tizim hisobotlarida aks etadi.</p>
                </div>
                <div class="form-grid">
                  <div class="form-group span-2">
                    <label>Restoran nomi *</label>
                    <input type="text" class="pos-input" [(ngModel)]="restaurant.name" placeholder="Masalan: Qarshi Milliy Taomlar">
                  </div>
                  <div class="form-group span-2">
                    <label>Manzil</label>
                    <input type="text" class="pos-input" [(ngModel)]="restaurant.address" placeholder="Shahar, ko'cha, mo'ljal...">
                  </div>
                  <div class="form-group">
                    <label>Telefon raqam</label>
                    <input type="text" class="pos-input" [(ngModel)]="restaurant.phone" placeholder="+998 90 123 45 67">
                  </div>
                  <div class="form-group">
                    <label>Email</label>
                    <input type="email" class="pos-input" [(ngModel)]="restaurant.email" placeholder="info@restoran.uz">
                  </div>
                  <div class="form-group">
                    <label>INN / STIR raqami</label>
                    <input type="text" class="pos-input" [(ngModel)]="restaurant.taxNumber" placeholder="301234567">
                  </div>
                  <div class="form-group">
                    <label>Veb-sayt</label>
                    <input type="text" class="pos-input" [(ngModel)]="restaurant.website" placeholder="https://restoran.uz">
                  </div>
                  <div class="form-group">
                    <label>Birlamchi valyuta</label>
                    <select class="pos-select" [(ngModel)]="restaurant.currency">
                      <option value="UZS">UZS (so'm)</option>
                      <option value="USD">USD ($)</option>
                      <option value="EUR">EUR (€)</option>
                      <option value="RUB">RUB (₽)</option>
                    </select>
                  </div>
                  <div class="form-group">
                    <label>Vaqt mintaqasi</label>
                    <input type="text" class="pos-input" [(ngModel)]="restaurant.timezone" value="Asia/Tashkent" readonly>
                  </div>
                  <div class="form-group span-2">
                    <label>Ish vaqti</label>
                    <input type="text" class="pos-input" [(ngModel)]="restaurant.workingHours" placeholder="09:00 - 23:00">
                  </div>
                  <div class="form-group span-2">
                    <label>Restoran tavsifi</label>
                    <textarea class="pos-textarea" [(ngModel)]="restaurant.description" rows="3" placeholder="Restoran haqida qisqacha ma'lumot..."></textarea>
                  </div>
                </div>
              </div>
            }

            <!-- 2. GENERAL SETTINGS -->
            @if (activeCategory() === 'GENERAL') {
              <div class="category-card">
                <div class="card-header">
                  <h3>🌐 Umumiy Tizim Sozlamalari</h3>
                  <p>Foydalanuvchi interfeysi, sanalar va tasdiqlash bildirishnomalari.</p>
                </div>
                <div class="form-grid">
                  <div class="form-group">
                    <label>Tizim tili</label>
                    <select class="pos-select" [(ngModel)]="general.language">
                      <option value="uz">O'zbekcha</option>
                      <option value="ru">Русский</option>
                      <option value="en">English</option>
                    </select>
                  </div>
                  <div class="form-group">
                    <label>Tizim mavzusi (Theme)</label>
                    <select class="pos-select" [(ngModel)]="general.theme">
                      <option value="dark">🌙 Dark Mode (Tungi rejim)</option>
                      <option value="light">☀️ Light Mode (Kunduzgi rejim)</option>
                    </select>
                  </div>
                  <div class="form-group">
                    <label>Sana formati</label>
                    <input type="text" class="pos-input" [(ngModel)]="general.dateFormat" placeholder="DD.MM.YYYY">
                  </div>
                  <div class="form-group">
                    <label>Vaqt formati</label>
                    <input type="text" class="pos-input" [(ngModel)]="general.timeFormat" placeholder="HH:mm">
                  </div>

                  <div class="form-group span-2">
                    <div class="toggle-list">
                      <label class="toggle-item">
                        <input type="checkbox" [(ngModel)]="general.autoSave">
                        <div>
                          <strong>Avtomatik saqlash (Auto Save)</strong>
                          <p>O'zgarishlar kiritilganda formalar avtomatik saqlanadi</p>
                        </div>
                      </label>
                      <label class="toggle-item">
                        <input type="checkbox" [(ngModel)]="general.autoRefresh">
                        <div>
                          <strong>Avtomatik yangilash (Auto Refresh)</strong>
                          <p>Buyurtma va stollar ro'yxatini vaqti-vaqti bilan avtomatik yangilash</p>
                        </div>
                      </label>
                      <label class="toggle-item">
                        <input type="checkbox" [(ngModel)]="general.realTimeUpdates">
                        <div>
                          <strong>Real-time WebSocket ulanish</strong>
                          <p>Oshxona va kassa o'rtasida buyurtmalarni jonli sinxronizatsiya qilish</p>
                        </div>
                      </label>
                      <label class="toggle-item">
                        <input type="checkbox" [(ngModel)]="general.confirmBeforeCancelOrder">
                        <div>
                          <strong>Buyurtmani bekor qilishdan oldin tasdiqlash</strong>
                          <p>Xatolik bilan bekor qilinishining oldini oladi</p>
                        </div>
                      </label>
                      <label class="toggle-item">
                        <input type="checkbox" [(ngModel)]="general.confirmBeforePayment">
                        <div>
                          <strong>To'lovni tasdiqlash dialogi</strong>
                          <p>To'lov yakunlanishidan oldin qo'shimcha tasdiq so'rash</p>
                        </div>
                      </label>
                      <label class="toggle-item">
                        <input type="checkbox" [(ngModel)]="general.confirmBeforeDelete">
                        <div>
                          <strong>O'chirishdan oldin tasdiqlash</strong>
                          <p>Mahsulot yoki ma'lumotlarni o'chirishda tasdiq oynasi chiqadi</p>
                        </div>
                      </label>
                    </div>
                  </div>
                </div>
              </div>
            }

            <!-- 3. USERS & ROLES -->
            @if (activeCategory() === 'USERS') {
              <div class="category-card">
                <div class="card-header">
                  <h3>👥 Rollar va Xavfsizlik Ruxsatlari</h3>
                  <p>Lavozimlar bo'yicha ruxsat etilgan huquqlar (RBAC tizimi).</p>
                </div>
                <div class="roles-grid">
                  <div class="role-card admin">
                    <div class="role-badge">ADMIN</div>
                    <h4>Administrator</h4>
                    <p>Tizimning to'liq boshqaruvi, barcha sozlamalar, hisobotlar va xodimlar.</p>
                    <div class="permission-tags">
                      <span class="perm-tag full">BARCHA HUQUQLAR (100%)</span>
                    </div>
                  </div>
                  <div class="role-card manager">
                    <div class="role-badge">MANAGER</div>
                    <h4>Restoran Menejeri</h4>
                    <p>Buyurtmalar, chegirmalar, stollar, bekor qilishlar va operatsion sozlamalar.</p>
                    <div class="permission-tags">
                      <span class="perm-tag">MANAGE_SETTINGS</span>
                      <span class="perm-tag">EDIT_ORDER</span>
                      <span class="perm-tag">VIEW_REPORTS</span>
                      <span class="perm-tag">MANAGE_TABLES</span>
                    </div>
                  </div>
                  <div class="role-card cashier">
                    <div class="role-badge">CASHIER</div>
                    <h4>Kassir</h4>
                    <p>Buyurtma to'lovlarini qabul qilish va kassa cheki chop etish.</p>
                    <div class="permission-tags">
                      <span class="perm-tag">PROCESS_PAYMENT</span>
                      <span class="perm-tag">PRINT_RECEIPT</span>
                    </div>
                  </div>
                  <div class="role-card waiter">
                    <div class="role-badge">WAITER</div>
                    <h4>Ofitsiant</h4>
                    <p>Stollardan buyurtma olish, taom qo'shish va oshxonaga jo'natish.</p>
                    <div class="permission-tags">
                      <span class="perm-tag">CREATE_ORDER</span>
                      <span class="perm-tag">EDIT_ORDER</span>
                      <span class="perm-tag">SEND_TO_KITCHEN</span>
                    </div>
                  </div>
                  <div class="role-card kitchen">
                    <div class="role-badge">KITCHEN</div>
                    <h4>Oshpaz / Stansiya</h4>
                    <p>Faqat o'ziga tegishli oshxona stansiyasi buyurtmalarini ko'rish va tayyorlash.</p>
                    <div class="permission-tags">
                      <span class="perm-tag">KITCHEN_VIEW</span>
                      <span class="perm-tag">KITCHEN_UPDATE</span>
                    </div>
                  </div>
                </div>
              </div>
            }

            <!-- 4. TABLES OVERVIEW -->
            @if (activeCategory() === 'TABLES') {
              <div class="category-card">
                <div class="card-header">
                  <h3>🪑 Stollar va Zallar Holati</h3>
                  <p>Restorandagi zallar va o'rindiqlar sxemasi.</p>
                </div>
                <div class="stats-cards-row">
                  <div class="stat-box">
                    <span class="stat-val">10</span>
                    <span class="stat-lbl">Jami stollar</span>
                  </div>
                  <div class="stat-box">
                    <span class="stat-val">Asosiy zal</span>
                    <span class="stat-lbl">Asosiy hudud</span>
                  </div>
                  <div class="stat-box">
                    <span class="stat-val">Faol</span>
                    <span class="stat-lbl">Holati</span>
                  </div>
                </div>
                <div class="info-alert">
                  Stollarni ko'chirish, o'lchamini o'zgartirish va holatini boshqarish uchun chap menyudagi <strong>🪑 Tables</strong> bo'limiga o'ting.
                </div>
              </div>
            }

            <!-- 5. PRODUCTS OVERVIEW -->
            @if (activeCategory() === 'PRODUCTS') {
              <div class="category-card">
                <div class="card-header">
                  <h3>🍔 Mahsulotlar va Oshxona Bog'lanishi</h3>
                  <p>Har bir taom oshxona stansiyasiga bog'lanadi (Product → Kitchen → Printer).</p>
                </div>
                <div class="info-alert">
                  Mahsulotlar va toifalarni to'liq boshqarish uchun chap menyudagi <strong>🍔 Products</strong> bo'limiga o'ting.
                  Har bir mahsulot tahrirlanganda uning <strong>Oshxona bo'limi (Kitchen)</strong> to'g'ri tanlanganligiga ishonch hosil qiling.
                </div>
              </div>
            }

            <!-- 6. KITCHENS / DEPARTMENTS -->
            @if (activeCategory() === 'KITCHENS') {
              <div class="category-card">
                <div class="card-header-flex">
                  <div>
                    <h3>👨‍🍳 Oshxonalar va Bo'limlar Sozlamalari</h3>
                    <p>Taomlar tayyorlanadigan stansiyalar va ularning shaxsiy printerlari.</p>
                  </div>
                  <button class="pos-btn pos-btn-secondary" (click)="openAddKitchenModal()">+ Yangi Bo'lim</button>
                </div>

                <div class="kitchens-table-container">
                  <table class="pos-table">
                    <thead>
                      <tr>
                        <th>Nomi</th>
                        <th>Kodi</th>
                        <th>Rangi</th>
                        <th>Biriktirilgan Printer</th>
                        <th>Auto Print</th>
                        <th>Ovoz</th>
                        <th>Tayyorlash vaqti</th>
                        <th>Amallar</th>
                      </tr>
                    </thead>
                    <tbody>
                      @for (k of kitchens(); track k.id) {
                        <tr>
                          <td>
                            <strong>{{ k.name }}</strong>
                            <div class="muted-small">{{ k.description || "Ta'rifi yo'q" }}</div>
                          </td>
                          <td><code>{{ k.code }}</code></td>
                          <td>
                            <span class="color-badge" [style.background-color]="k.color || '#6366F1'"></span>
                          </td>
                          <td>
                            @if (k.printerName) {
                              <span class="printer-tag" [class.online]="k.printerStatus === 'ONLINE'">
                                🖨️ {{ k.printerName }}
                              </span>
                            } @else {
                              <span class="printer-tag offline">❌ Printer ulanmagan</span>
                            }
                          </td>
                          <td>
                            <span class="status-pill" [class.active]="k.autoPrint">
                              {{ k.autoPrint ? 'ON' : 'OFF' }}
                            </span>
                          </td>
                          <td>
                            <span class="status-pill" [class.active]="k.soundNotification">
                              {{ k.soundNotification ? 'ON' : 'OFF' }}
                            </span>
                          </td>
                          <td>{{ k.preparationTimeMinutes || 15 }} daqiqa</td>
                          <td>
                            <button class="pos-btn-sm" (click)="editKitchen(k)">Tahrirlash</button>
                          </td>
                        </tr>
                      }
                    </tbody>
                  </table>
                </div>
              </div>
            }

            <!-- 7. PRINTER MANAGEMENT (ENG MUHIM QISM) -->
            @if (activeCategory() === 'PRINTERS') {
              <div class="category-card">
                <div class="card-header-flex">
                  <div>
                    <h3>🖨️ Restoran Printer Management Tizimi</h3>
                    <p>Kompyuterdagi real Windows printerlari bilan to'g'ridan-to'g'ri integratsiya va oshxona routingi.</p>
                  </div>
                  <div class="action-btn-group">
                    <button class="pos-btn pos-btn-secondary" (click)="refreshPrinters()" [disabled]="refreshingPrinters()">
                      @if (refreshingPrinters()) {
                        <span class="spinner"></span> Yangilanmoqda...
                      } @else {
                        ↻ Printerlarni yangilash
                      }
                    </button>
                    <button class="pos-btn pos-btn-primary" (click)="openAddPrinterModal()">+ Printer qo'shish</button>
                  </div>
                </div>

                <!-- STATS ROW -->
                <div class="stats-cards-row">
                  <div class="stat-box">
                    <span class="stat-val">{{ printers().length }}</span>
                    <span class="stat-lbl">Jami printerlar</span>
                  </div>
                  <div class="stat-box success">
                    <span class="stat-val">{{ onlinePrintersCount() }}</span>
                    <span class="stat-lbl">ONLINE printerlar</span>
                  </div>
                  <div class="stat-box danger">
                    <span class="stat-val">{{ offlinePrintersCount() }}</span>
                    <span class="stat-lbl">OFFLINE / NOT FOUND</span>
                  </div>
                  <div class="stat-box">
                    <span class="stat-val">{{ defaultCashierPrinterName() }}</span>
                    <span class="stat-lbl">Asosiy Kassa Printeri</span>
                  </div>
                </div>

                <!-- TEST PRINT FEEDBACK BANNER -->
                @if (testPrintResult()) {
                  <div class="test-feedback-box" [class.success]="testPrintResult()?.success" [class.error]="!testPrintResult()?.success">
                    <div class="feedback-header">
                      @if (testPrintResult()?.success) {
                        <strong class="feedback-title success-title">✅ TEST CHOP ETISH MUVAFFAQIYATLI</strong>
                      } @else {
                        <strong class="feedback-title error-title">❌ PRINTERGA ULANISH IMKONI BO'LMADI</strong>
                      }
                      <button class="toast-close" (click)="clearTestResult()">✕</button>
                    </div>
                    <div class="feedback-body">
                      <div><strong>Printer:</strong> {{ testPrintResult()?.printerName }}</div>
                      <div><strong>Windows printeri:</strong> {{ testPrintResult()?.target }}</div>
                      <div><strong>Xabar:</strong> {{ testPrintResult()?.message }}</div>
                      @if (testPrintResult()?.errorDetails) {
                        <div class="error-cause"><strong>Xato sababi:</strong> {{ testPrintResult()?.errorDetails }}</div>
                      }
                    </div>
                  </div>
                }

                <!-- PRINTERS GRID -->
                <div class="printers-grid">
                  @for (p of printers(); track p.id) {
                    <div class="printer-card" [class.is-default]="p.isDefault">
                      <div class="printer-card-top">
                        <div>
                          <div class="printer-name">
                            🖨️ {{ p.name }}
                            @if (p.isDefault) {
                              <span class="default-badge">ASOSIY KASSA</span>
                            }
                          </div>
                          <div class="printer-model">{{ p.model || 'Windows Print Device' }}</div>
                        </div>
                        <span class="status-indicator" [class.online]="p.status === 'ONLINE'" [class.offline]="p.status === 'OFFLINE'" [class.notfound]="p.status === 'NOT_FOUND'" [class.unknown]="p.status === 'UNKNOWN'">
                          ● {{ p.status }}
                        </span>
                      </div>

                      <div class="printer-details">
                        <div class="detail-row">
                          <span class="lbl">Windows printeri:</span>
                          <span class="val font-semibold">{{ p.systemPrinterName || p.windowsPrinterName || 'Nomalum' }}</span>
                        </div>
                        <div class="detail-row">
                          <span class="lbl">Maqsadi (Purpose):</span>
                          <span class="val purpose-tag" [class.kitchen]="p.purpose === 'KITCHEN'" [class.cashier]="p.purpose === 'CASHIER'">
                            {{ p.purpose }}
                          </span>
                        </div>
                        @if (p.purpose === 'KITCHEN') {
                          <div class="detail-row">
                            <span class="lbl">Oshxona bo'limi:</span>
                            <span class="val">
                              @if (p.assignedKitchenName) {
                                <strong>{{ p.assignedKitchenName }}</strong>
                                @if (p.isPrimaryForKitchen) {
                                  <span class="primary-mini-pill">(PRIMARY)</span>
                                }
                              } @else {
                                <span class="muted-small">Biriktirilmagan</span>
                              }
                            </span>
                          </div>
                        }
                        <div class="detail-row">
                          <span class="lbl">Qog'oz formati:</span>
                          <span class="val">{{ p.paperWidth }} mm</span>
                        </div>
                        <div class="detail-row">
                          <span class="lbl">Auto-Print:</span>
                          <span class="val" [class.text-success]="p.autoPrint">{{ p.autoPrint ? 'Yoqilgan (ON)' : 'Ochirilgan (OFF)' }}</span>
                        </div>
                        @if (p.fallbackPrinterName) {
                          <div class="detail-row">
                            <span class="lbl">Zaxira (Fallback):</span>
                            <span class="val fallback-tag">🔄 {{ p.fallbackPrinterName }}</span>
                          </div>
                        }
                        @if (p.lastError) {
                          <div class="last-error-box">
                            ⚠️ {{ p.lastError }}
                          </div>
                        }
                      </div>

                      <div class="printer-card-actions">
                        <button class="pos-btn-sm btn-test" (click)="testPrintPrinter(p)" [disabled]="testingId() === p.id">
                          @if (testingId() === p.id) {
                            <span class="spinner"></span> Sinov...
                          } @else {
                            ⚡ TEST PRINT
                          }
                        </button>
                        <button class="pos-btn-sm" (click)="editPrinter(p)">Tahrirlash</button>
                        <button class="pos-btn-sm btn-danger" (click)="confirmDeletePrinter(p)">O'chirish</button>
                      </div>
                    </div>
                  }
                </div>
              </div>
            }

            <!-- 8. RECEIPT / CHEQUE SETTINGS -->
            @if (activeCategory() === 'RECEIPT') {
              <div class="category-card">
                <div class="card-header">
                  <h3>🧾 Kassa Cheki va Kvitansiya Sozlamalari</h3>
                  <p>Mijozlarga beriladigan to'lov cheki formati va rekvizitlari.</p>
                </div>
                <div class="receipt-settings-split">
                  <!-- LEFT: FORM -->
                  <div class="receipt-form">
                    <div class="form-group">
                      <label>Chek yuqori sarlavhasi (Header)</label>
                      <input type="text" class="pos-input" [(ngModel)]="receipt.header">
                    </div>
                    <div class="form-group">
                      <label>Chek pastki yozuvi (Footer)</label>
                      <input type="text" class="pos-input" [(ngModel)]="receipt.footer">
                    </div>
                    <div class="form-group">
                      <label>Qog'oz o'lchami</label>
                      <select class="pos-select" [(ngModel)]="receipt.paperWidth">
                        <option [ngValue]="80">80 mm (Standart POS cheki)</option>
                        <option [ngValue]="58">58 mm (Kichik lenta)</option>
                      </select>
                    </div>
                    <div class="form-group">
                      <label>Nusxalar soni</label>
                      <input type="number" class="pos-input" [(ngModel)]="receipt.numberOfCopies" min="1" max="5">
                    </div>

                    <div class="toggle-list">
                      <label class="toggle-item">
                        <input type="checkbox" [(ngModel)]="receipt.showWaiter">
                        <span>Ofitsiant ismini ko'rsatish</span>
                      </label>
                      <label class="toggle-item">
                        <input type="checkbox" [(ngModel)]="receipt.showCashier">
                        <span>Kassir ismini ko'rsatish</span>
                      </label>
                      <label class="toggle-item">
                        <input type="checkbox" [(ngModel)]="receipt.showTable">
                        <span>Stol raqamini ko'rsatish</span>
                      </label>
                      <label class="toggle-item">
                        <input type="checkbox" [(ngModel)]="receipt.showOrderNumber">
                        <span>Buyurtma raqamini ko'rsatish</span>
                      </label>
                      <label class="toggle-item">
                        <input type="checkbox" [(ngModel)]="receipt.showDateTime">
                        <span>Sana va vaqtni ko'rsatish</span>
                      </label>
                      <label class="toggle-item">
                        <input type="checkbox" [(ngModel)]="receipt.showPaymentMethod">
                        <span>To'lov usulini ko'rsatish</span>
                      </label>
                      <label class="toggle-item">
                        <input type="checkbox" [(ngModel)]="receipt.showDiscount">
                        <span>Chegirmani ko'rsatish</span>
                      </label>
                      <label class="toggle-item">
                        <input type="checkbox" [(ngModel)]="receipt.showServiceCharge">
                        <span>Xizmat haqini ko'rsatish</span>
                      </label>
                      <label class="toggle-item">
                        <input type="checkbox" [(ngModel)]="receipt.showTax">
                        <span>Soliqni (QQS) ko'rsatish</span>
                      </label>
                    </div>
                  </div>

                  <!-- RIGHT: LIVE RECEIPT PREVIEW -->
                  <div class="receipt-preview-container">
                    <div class="preview-label">JONLI CHEK NAMUNASI (PREVIEW)</div>
                    <div class="receipt-paper" [class.narrow]="receipt.paperWidth === 58">
                      <div class="receipt-p-center bold">{{ restaurant.name || 'OYBEK RESTAURANT' }}</div>
                      <div class="receipt-p-center muted-small">{{ restaurant.address || 'Qarshi shahri' }}</div>
                      <div class="receipt-p-center muted-small">Tel: {{ restaurant.phone || '+998 90 123 45 67' }}</div>
                      <div class="receipt-p-center small-italic">{{ receipt.header }}</div>
                      <div class="receipt-divider">================================</div>

                      @if (receipt.showOrderNumber) {
                        <div class="receipt-row"><span>CHEK:</span> <span>PAY-20260911-0001</span></div>
                      }
                      @if (receipt.showTable) {
                        <div class="receipt-row"><span>STOL:</span> <span>Stol №5</span></div>
                      }
                      @if (receipt.showWaiter) {
                        <div class="receipt-row"><span>OFITSIANT:</span> <span>Ali Valiyev</span></div>
                      }
                      @if (receipt.showCashier) {
                        <div class="receipt-row"><span>KASSIR:</span> <span>Jasur</span></div>
                      }
                      @if (receipt.showDateTime) {
                        <div class="receipt-row"><span>SANA:</span> <span>11.09.2026 14:30</span></div>
                      }
                      <div class="receipt-divider">--------------------------------</div>
                      <div class="receipt-row bold"><span>MAHSULOT</span> <span>SUMMA</span></div>
                      <div class="receipt-divider">--------------------------------</div>
                      <div class="receipt-row"><span>Osh Choyxona x2</span> <span>90 000</span></div>
                      <div class="receipt-row"><span>Tandir Somsa x3</span> <span>30 000</span></div>
                      <div class="receipt-row"><span>Coca-Cola 1.5L x1</span> <span>15 000</span></div>
                      <div class="receipt-divider">--------------------------------</div>
                      <div class="receipt-row"><span>ORALIQ JAMI:</span> <span>135 000 so'm</span></div>
                      @if (receipt.showDiscount) {
                        <div class="receipt-row"><span>CHEGIRMA (5%):</span> <span>-6 750 so'm</span></div>
                      }
                      @if (receipt.showServiceCharge) {
                        <div class="receipt-row"><span>XIZMAT HAQI (10%):</span> <span>+13 500 so'm</span></div>
                      }
                      @if (receipt.showTax) {
                        <div class="receipt-row"><span>SOLIQ (QQS):</span> <span>0 so'm</span></div>
                      }
                      <div class="receipt-divider">================================</div>
                      <div class="receipt-row grand-total"><span>JAMI:</span> <span>141 750 so'm</span></div>
                      @if (receipt.showPaymentMethod) {
                        <div class="receipt-row"><span>TO'LOV:</span> <span>NAQD PUL</span></div>
                      }
                      <div class="receipt-divider">================================</div>
                      <div class="receipt-p-center small-italic">{{ receipt.footer }}</div>
                    </div>
                  </div>
                </div>
              </div>
            }

            <!-- 9. PAYMENTS SETTINGS -->
            @if (activeCategory() === 'PAYMENTS') {
              <div class="category-card">
                <div class="card-header">
                  <h3>💳 To'lov Usullari va Kassa Integratsiyasi</h3>
                  <p>Mijozlardan qabul qilinadigan to'lov tizimlari sozlamalari.</p>
                </div>
                <div class="form-grid">
                  <div class="form-group">
                    <label>Birlamchi to'lov usuli</label>
                    <select class="pos-select" [(ngModel)]="payments.defaultPaymentMethod">
                      <option value="CASH">Naqd pul (CASH)</option>
                      <option value="CARD">Bank kartasi (Humo/Uzcard)</option>
                      <option value="CLICK">Click</option>
                      <option value="PAYME">Payme</option>
                    </select>
                  </div>
                  <div class="form-group span-2">
                    <div class="toggle-list">
                      <label class="toggle-item">
                        <input type="checkbox" [(ngModel)]="payments.cashEnabled">
                        <div>
                          <strong>Naqd pul to'lovi (CASH)</strong>
                          <p>Kassada naqd pul bilan to'lov qilish imkoniyati</p>
                        </div>
                      </label>
                      <label class="toggle-item">
                        <input type="checkbox" [(ngModel)]="payments.cardEnabled">
                        <div>
                          <strong>Bank kartalari (CARD / POS Terminal)</strong>
                          <p>Uzcard, Humo, Visa, Mastercard to'lovlari</p>
                        </div>
                      </label>
                      <label class="toggle-item">
                        <input type="checkbox" [(ngModel)]="payments.clickEnabled">
                        <div>
                          <strong>Click to'lov tizimi</strong>
                          <p>QR-kod yoki Click ilovasi orqali to'lov</p>
                        </div>
                      </label>
                      <label class="toggle-item">
                        <input type="checkbox" [(ngModel)]="payments.paymeEnabled">
                        <div>
                          <strong>Payme to'lov tizimi</strong>
                          <p>Payme QR orqali to'lovlar</p>
                        </div>
                      </label>
                      <label class="toggle-item">
                        <input type="checkbox" [(ngModel)]="payments.allowMixedPayment">
                        <div>
                          <strong>Aralash to'lov (Split payment)</strong>
                          <p>Buyurtma summasini bir vaqtning o'zida naqd va karta orqali to'lash</p>
                        </div>
                      </label>
                      <label class="toggle-item">
                        <input type="checkbox" [(ngModel)]="payments.autoPrintReceiptAfterPayment">
                        <div>
                          <strong>To'lovdan keyin kassa chekini avtomatik chiqarish</strong>
                          <p>To'lov muvaffaqiyatli bo'lishi bilan kassa printeri avtomatik chop etadi</p>
                        </div>
                      </label>
                    </div>
                  </div>
                </div>
              </div>
            }

            <!-- 10. TAX & SERVICE CHARGE -->
            @if (activeCategory() === 'TAX_SERVICE') {
              <div class="category-card">
                <div class="card-header">
                  <h3>📊 Xizmat Haqi va Soliq Foizlari</h3>
                  <p>Restoran xizmati va davlat soliqlari avtomatik hisob-kitobi.</p>
                </div>
                <div class="form-grid">
                  <div class="form-group">
                    <label class="checkbox-label">
                      <input type="checkbox" [(ngModel)]="taxService.serviceChargeEnabled">
                      <strong>Xizmat haqi (Service Charge) faol</strong>
                    </label>
                    <div class="input-with-addon">
                      <input type="number" class="pos-input" [(ngModel)]="taxService.serviceChargePercent" [disabled]="!taxService.serviceChargeEnabled" min="0" max="100" step="0.5">
                      <span class="addon">%</span>
                    </div>
                    <span class="muted-small">Masalan: 10% yoki 15%</span>
                  </div>

                  <div class="form-group">
                    <label class="checkbox-label">
                      <input type="checkbox" [(ngModel)]="taxService.taxEnabled">
                      <strong>Soliq (QQS / Tax) faol</strong>
                    </label>
                    <div class="input-with-addon">
                      <input type="number" class="pos-input" [(ngModel)]="taxService.taxPercent" [disabled]="!taxService.taxEnabled" min="0" max="100" step="0.5">
                      <span class="addon">%</span>
                    </div>
                    <span class="muted-small">Masalan: 12% QQS</span>
                  </div>
                </div>
              </div>
            }

            <!-- 11. ORDER SETTINGS -->
            @if (activeCategory() === 'ORDERS') {
              <div class="category-card">
                <div class="card-header">
                  <h3>📋 Buyurtmalar Ishlash Tartibi</h3>
                  <p>Buyurtma raqamlash, bekor qilish va tahrirlash qoidalari.</p>
                </div>
                <div class="form-grid">
                  <div class="form-group">
                    <label>Buyurtma raqami prefiksi</label>
                    <input type="text" class="pos-input" [(ngModel)]="orders.orderNumberPrefix" placeholder="ORD">
                  </div>
                  <div class="form-group span-2">
                    <div class="toggle-list">
                      <label class="toggle-item">
                        <input type="checkbox" [(ngModel)]="orders.autoOrderNumber">
                        <div><strong>Avtomatik ketma-ket raqamlash</strong><p>ORD-YYYYMMDD-XXXX ko'rinishida generatsiya qilish</p></div>
                      </label>
                      <label class="toggle-item">
                        <input type="checkbox" [(ngModel)]="orders.allowOrderEditing">
                        <div><strong>Ochiq buyurtmani tahrirlashga ruxsat</strong><p>Band stolga qo'shimcha taomlar qo'shish imkoniyati</p></div>
                      </label>
                      <label class="toggle-item">
                        <input type="checkbox" [(ngModel)]="orders.allowItemCancellation">
                        <div><strong>Taomni bekor qilishga ruxsat</strong><p>Oshxonaga yuborilgan taomni sabab bilan bekor qilish</p></div>
                      </label>
                      <label class="toggle-item">
                        <input type="checkbox" [(ngModel)]="orders.requireCancellationReason">
                        <div><strong>Bekor qilish sababi majburiyligi</strong><p>Bekor qilishda audit uchun sabab kiritish talab qilinadi</p></div>
                      </label>
                      <label class="toggle-item">
                        <input type="checkbox" [(ngModel)]="orders.autoSendToKitchen">
                        <div><strong>Avtomatik oshxonaga jo'natish</strong><p>Yangi buyurtma saqlanishi bilan tegishli oshxonalarga yo'naltiriladi</p></div>
                      </label>
                      <label class="toggle-item">
                        <input type="checkbox" [(ngModel)]="orders.allowSplitBill">
                        <div><strong>Hisobni bo'lishga ruxsat (Split Bill)</strong><p>Mijozlar alohida to'lashlari uchun buyurtmani bo'lish</p></div>
                      </label>
                    </div>
                  </div>
                </div>
              </div>
            }

            <!-- 12. KITCHEN DISPLAY (KDS) -->
            @if (activeCategory() === 'KITCHEN_DISPLAY') {
              <div class="category-card">
                <div class="card-header">
                  <h3>🍳 Oshxona Ekrani (KDS) Sozlamalari</h3>
                  <p>Oshpazlar monitori va ticketlar ko'rinishi parametrlari.</p>
                </div>
                <div class="form-grid">
                  <div class="form-group">
                    <label>Ticket shrift hajmi</label>
                    <select class="pos-select" [(ngModel)]="kitchenDisplay.ticketFontSize">
                      <option value="small">Kichik (Small)</option>
                      <option value="medium">O'rtacha (Medium)</option>
                      <option value="large">Katta (Large - oshpazlar uchun qulay)</option>
                    </select>
                  </div>
                  <div class="form-group span-2">
                    <div class="toggle-list">
                      <label class="toggle-item">
                        <input type="checkbox" [(ngModel)]="kitchenDisplay.autoPrintKitchenOrder">
                        <div><strong>Oshxona ticketini avtomatik chop etish</strong><p>Buyurtma berilganda printerdan chek chiqadi</p></div>
                      </label>
                      <label class="toggle-item">
                        <input type="checkbox" [(ngModel)]="kitchenDisplay.soundNotificationOnNewTicket">
                        <div><strong>Yangi buyurtma kelganda audio signal</strong><p>Oshpaz diqqatini tortish uchun ovozli ogohlantirish</p></div>
                      </label>
                      <label class="toggle-item">
                        <input type="checkbox" [(ngModel)]="kitchenDisplay.showWaiterOnTicket">
                        <div><strong>Ticketda ofitsiant ismini ko'rsatish</strong></div>
                      </label>
                      <label class="toggle-item">
                        <input type="checkbox" [(ngModel)]="kitchenDisplay.showTableOnTicket">
                        <div><strong>Ticketda stol raqamini ko'rsatish</strong></div>
                      </label>
                      <label class="toggle-item">
                        <input type="checkbox" [(ngModel)]="kitchenDisplay.showNotesOnTicket">
                        <div><strong>Ticketda maxsus eslatmalarni (izohlarni) ko'rsatish</strong></div>
                      </label>
                    </div>
                  </div>
                </div>
              </div>
            }

            <!-- 13. NOTIFICATIONS -->
            @if (activeCategory() === 'NOTIFICATIONS') {
              <div class="category-card">
                <div class="card-header">
                  <h3>🔔 Bildirishnomalar va Audio Signallar</h3>
                  <p>Yangi buyurtmalar va taomlar tayyor bo'lganda ovozli xabarlar.</p>
                </div>
                <div class="form-grid">
                  <div class="form-group span-2">
                    <label class="checkbox-label">
                      <input type="checkbox" [(ngModel)]="notifications.soundEnabled">
                      <strong>Tizim audio signallari yoqilgan</strong>
                    </label>
                  </div>
                  <div class="form-group">
                    <label>Ovoz balandligi: {{ notifications.soundVolume }}%</label>
                    <input type="range" class="pos-range" [(ngModel)]="notifications.soundVolume" min="0" max="100">
                  </div>
                  <div class="form-group">
                    <label>Yangi buyurtma signali</label>
                    <select class="pos-select" [(ngModel)]="notifications.newOrderSound">
                      <option value="chime">Qo'ng'iroq (Chime)</option>
                      <option value="bell">Zang (Bell)</option>
                      <option value="beep">Elektron signal (Beep)</option>
                    </select>
                  </div>
                  <div class="form-group span-2">
                    <label class="toggle-item">
                      <input type="checkbox" [(ngModel)]="notifications.lowStockAlert">
                      <div><strong>Omborda xomashyo kam qolganda ogohlantirish</strong></div>
                    </label>
                  </div>
                </div>
              </div>
            }

            <!-- 14. SECURITY -->
            @if (activeCategory() === 'SECURITY') {
              <div class="category-card">
                <div class="card-header">
                  <h3>🔐 Xavfsizlik va Kirish Nazorati</h3>
                  <p>Kassir PIN kodlari va sessiya xavfsizligi qoidalari.</p>
                </div>
                <div class="form-grid">
                  <div class="form-group span-2">
                    <label class="toggle-item">
                      <input type="checkbox" [(ngModel)]="security.requirePinForCashier">
                      <div>
                        <strong>Kassir operatsiyalari uchun PIN so'rash</strong>
                        <p>To'lov va chegirmani tasdiqlash uchun 4 xonali PIN talab qilinadi</p>
                      </div>
                    </label>
                  </div>
                  <div class="form-group">
                    <label>Avtomatik chiqish (Auto logout) vaqti</label>
                    <div class="input-with-addon">
                      <input type="number" class="pos-input" [(ngModel)]="security.autoLogoutMinutes" min="5" max="240">
                      <span class="addon">daqiqa</span>
                    </div>
                  </div>
                  <div class="form-group">
                    <label>Maksimal noto'g'ri urinishlar</label>
                    <input type="number" class="pos-input" [(ngModel)]="security.maxLoginAttempts" min="3" max="10">
                  </div>
                </div>
              </div>
            }

            <!-- 15. BACKUP -->
            @if (activeCategory() === 'BACKUP') {
              <div class="category-card">
                <div class="card-header">
                  <h3>💾 Ma'lumotlar Bazasi va Zaxiralash (Backup)</h3>
                  <p>Mahalliy PostgreSQL bazasining xavfsizligi va zaxira nusxalari.</p>
                </div>
                <div class="stats-cards-row">
                  <div class="stat-box success">
                    <span class="stat-val">ONLINE</span>
                    <span class="stat-lbl">Baza holati: PostgreSQL 18</span>
                  </div>
                  <div class="stat-box">
                    <span class="stat-val">{{ backup.lastBackupTime }}</span>
                    <span class="stat-lbl">Oxirgi zaxira vaqti</span>
                  </div>
                  <div class="stat-box">
                    <span class="stat-val">{{ backup.dbSize }}</span>
                    <span class="stat-lbl">Baza hajmi</span>
                  </div>
                </div>

                <div class="form-grid" style="margin-top: 20px;">
                  <div class="form-group span-2">
                    <label>Zaxira fayllar saqlanadigan papka</label>
                    <input type="text" class="pos-input" [(ngModel)]="backup.backupLocation" readonly>
                  </div>
                  <div class="form-group">
                    <label>Avtomatik zaxiralash</label>
                    <select class="pos-select" [(ngModel)]="backup.backupFrequency">
                      <option value="DAILY">Har kuni tunda (00:00)</option>
                      <option value="WEEKLY">Haftada bir marta</option>
                      <option value="MANUAL">Faqat qo'lda</option>
                    </select>
                  </div>
                </div>

                <div class="backup-actions">
                  <button class="pos-btn pos-btn-primary" (click)="triggerManualBackup()" [disabled]="backingUp()">
                    @if (backingUp()) {
                      <span class="spinner"></span> Zaxira olinmoqda...
                    } @else {
                      ⚡ Hozir zaxira nusxa yaratish
                    }
                  </button>
                </div>
              </div>
            }

            <!-- 16. SYSTEM INFO & AUDIT LOG -->
            @if (activeCategory() === 'SYSTEM_INFO') {
              <div class="category-card">
                <div class="card-header">
                  <h3>ℹ️ Tizim Diagnostikasi va O'zgarishlar Tarixi</h3>
                  <p>Server parametrlari, servislar holati va sozlamalar audit qaydlari.</p>
                </div>

                <div class="stats-cards-row">
                  <div class="stat-box">
                    <span class="stat-val">{{ sysInfo()?.appVersion }}</span>
                    <span class="stat-lbl">Dastur versiyasi</span>
                  </div>
                  <div class="stat-box success">
                    <span class="stat-val">{{ sysInfo()?.backendStatus }}</span>
                    <span class="stat-lbl">Backend API</span>
                  </div>
                  <div class="stat-box">
                    <span class="stat-val">{{ sysInfo()?.postgresVersion }}</span>
                    <span class="stat-lbl">Ma'lumotlar bazasi</span>
                  </div>
                  <div class="stat-box">
                    <span class="stat-val">{{ sysInfo()?.onlinePrinters }}/{{ sysInfo()?.totalPrinters }}</span>
                    <span class="stat-lbl">Faol printerlar</span>
                  </div>
                </div>

                <div class="sys-details-list">
                  <div class="sys-item"><span>Server vaqti:</span> <strong>{{ sysInfo()?.serverTime }}</strong></div>
                  <div class="sys-item"><span>Frontend:</span> <strong>{{ sysInfo()?.frontendVersion }}</strong></div>
                  <div class="sys-item"><span>WebSocket:</span> <strong>{{ sysInfo()?.webSocketStatus }}</strong></div>
                  <div class="sys-item"><span>Xotira:</span> <strong>{{ sysInfo()?.freeMemoryMb }} MB bo'sh / {{ sysInfo()?.totalMemoryMb }} MB jami</strong></div>
                </div>

                <div class="audit-log-section">
                  <h4>📝 Oxirgi Sozlamalar Audit Tarixi (Kim, qachon, nima o'zgardi)</h4>
                  <div class="audit-table-wrapper">
                    <table class="pos-table">
                      <thead>
                        <tr>
                          <th>Vaqt</th>
                          <th>Xodim</th>
                          <th>Harakat</th>
                          <th>Bo'lim</th>
                          <th>Izoh</th>
                        </tr>
                      </thead>
                      <tbody>
                        @for (log of auditLogs(); track log.id) {
                          <tr>
                            <td class="muted-small">{{ log.createdAt | date:'dd.MM.yyyy HH:mm:ss' }}</td>
                            <td><strong>{{ log.userName }}</strong></td>
                            <td><span class="action-pill" [class.create]="log.action === 'CREATE'" [class.update]="log.action === 'UPDATE'">{{ log.action }}</span></td>
                            <td><code>{{ log.entityType }}</code></td>
                            <td>{{ log.notes }}</td>
                          </tr>
                        }
                      </tbody>
                    </table>
                  </div>
                </div>
              </div>
            }

          }
        </main>
      </div>

      <!-- ADD / EDIT PRINTER MODAL -->
      <!-- ADD / EDIT PRINTER MODAL -->
      @if (showPrinterModal()) {
        <div class="modal-backdrop">
          <div class="pos-modal printer-modal fade-in">
            <div class="modal-header">
              <div>
                <h3>{{ editingPrinter() ? 'Printerni Tahrirlash' : 'Kompyuterdagi mavjud printerlar' }}</h3>
                <p class="modal-subtitle">
                  {{ editingPrinter() ? 'POS printer parametrlarini sozlang' : 'Windows tizimidan aniqlangan printerni tanlang va konfiguratsiya qiling' }}
                </p>
              </div>
              <button class="modal-close" (click)="closePrinterModal()">✕</button>
            </div>
            <div class="modal-body">
              @if (!editingPrinter()) {
                <!-- 1. WINDOWS DISCOVERED PRINTERS LIST -->
                <div class="discovery-section">
                  <div class="discovery-header">
                    <span class="section-title">🔍 Windows tizimidagi o'rnatilgan printerlar:</span>
                    <button class="pos-btn-sm" (click)="loadAvailableWindowsPrinters()" [disabled]="loadingAvailablePrinters()">
                      @if (loadingAvailablePrinters()) {
                        <span class="spinner"></span> Qidirilmoqda...
                      } @else {
                        ↻ Qayta qidirish
                      }
                    </button>
                  </div>

                  @if (loadingAvailablePrinters()) {
                    <div class="discovery-loader">
                      <span class="spinner"></span> Windows tizimidagi printerlar skaner qilinmoqda...
                    </div>
                  } @else if (availableWindowsPrinters().length === 0) {
                    <div class="empty-printers-alert">
                      ⚠️ Ushbu kompyuterda Windows tomonidan o'rnatilgan printerlar topilmadi.
                      Iltimos, Windows "Printers & Scanners" menyusi orqali printerni o'rnating.
                    </div>
                  } @else {
                    <div class="windows-printers-list">
                      @for (wp of availableWindowsPrinters(); track wp.systemPrinterName) {
                        <div class="win-printer-card"
                             [class.selected]="selectedWindowsPrinter()?.systemPrinterName === wp.systemPrinterName"
                             (click)="selectWindowsPrinter(wp)">
                          <div class="win-printer-left">
                            <span class="radio-circle">{{ selectedWindowsPrinter()?.systemPrinterName === wp.systemPrinterName ? '●' : '○' }}</span>
                            <div>
                              <div class="win-printer-name">
                                🖨️ {{ wp.systemPrinterName }}
                                @if (wp.isDefault) {
                                  <span class="default-badge">Windows Asosiy</span>
                                }
                              </div>
                              <div class="win-printer-driver">Drayver: {{ wp.driverName || 'Standart' }}</div>
                            </div>
                          </div>
                          <span class="status-indicator" [class.online]="wp.status === 'ONLINE'" [class.offline]="wp.status === 'OFFLINE'" [class.unknown]="wp.status === 'UNKNOWN'">
                            ● {{ wp.status }}
                          </span>
                        </div>
                      }
                    </div>
                  }
                </div>
              }

              <!-- 2. POS CONFIGURATION FORM -->
              @if (editingPrinter() || selectedWindowsPrinter()) {
                <div class="config-divider"></div>
                <div class="selected-printer-pill">
                  <span>Tanlangan Windows printeri:</span>
                  <strong>{{ editingPrinter() ? (editingPrinter()?.systemPrinterName || editingPrinter()?.windowsPrinterName) : selectedWindowsPrinter()?.systemPrinterName }}</strong>
                </div>

                <div class="form-grid">
                  <div class="form-group span-2">
                    <label>Printer nomi (POS uchun ko'rinadigan nom)</label>
                    <input type="text" class="pos-input" [(ngModel)]="printerForm.name" placeholder="Masalan: EPSON TM-T20III">
                  </div>

                  <div class="form-group">
                    <label>Printer maqsadi (Purpose) *</label>
                    <select class="pos-select" [(ngModel)]="printerForm.purpose">
                      <option value="KITCHEN">KITCHEN (Oshxona)</option>
                      <option value="CASHIER">CASHIER (Kassa)</option>
                    </select>
                  </div>

                  @if (printerForm.purpose === 'KITCHEN') {
                    <div class="form-group">
                      <label>Oshxona bo'limi *</label>
                      <select class="pos-select" [(ngModel)]="printerForm.kitchenId">
                        <option [ngValue]="null">-- Oshxonani tanlang --</option>
                        @for (k of kitchens(); track k.id) {
                          <option [ngValue]="k.id">{{ k.name }} ({{ k.code }})</option>
                        }
                      </select>
                    </div>
                  }

                  <div class="form-group">
                    <label>Qog'oz formati *</label>
                    <select class="pos-select" [(ngModel)]="printerForm.paperWidth">
                      <option [ngValue]="80">80 mm (Standart)</option>
                      <option [ngValue]="58">58 mm (Kichik)</option>
                    </select>
                  </div>

                  <div class="form-group span-2">
                    <div class="toggle-list">
                      <label class="toggle-item">
                        <input type="checkbox" [(ngModel)]="printerForm.autoPrint">
                        <span>Avtomatik chop etish (Auto Print)</span>
                      </label>
                      @if (printerForm.purpose === 'KITCHEN') {
                        <label class="toggle-item">
                          <input type="checkbox" [(ngModel)]="printerForm.isPrimary">
                          <span>Ushbu oshxona uchun asosiy (PRIMARY) printer</span>
                        </label>
                      }
                      @if (printerForm.purpose === 'CASHIER') {
                        <label class="toggle-item">
                          <input type="checkbox" [(ngModel)]="printerForm.isDefault">
                          <span>Asosiy kassa printeri (PRIMARY)</span>
                        </label>
                      }
                    </div>
                  </div>
                </div>
              }
            </div>
            <div class="modal-footer">
              <button class="pos-btn pos-btn-secondary" (click)="closePrinterModal()">Bekor qilish</button>
              <button class="pos-btn pos-btn-primary" (click)="savePrinter()" [disabled]="savingPrinter() || (!editingPrinter() && !selectedWindowsPrinter())">
                @if (savingPrinter()) {
                  <span class="spinner"></span> Saqlanmoqda...
                } @else {
                  Saqlash
                }
              </button>
            </div>
          </div>
        </div>
      }

      <!-- DELETE CONFIRMATION MODAL -->
      @if (deletingPrinter()) {
        <div class="modal-backdrop">
          <div class="pos-modal confirm-modal fade-in">
            <div class="modal-header">
              <h3>Printerni o'chirish</h3>
              <button class="modal-close" (click)="cancelDeletePrinter()">✕</button>
            </div>
            <div class="modal-body">
              <p>Haqiqatan ham <strong>{{ deletingPrinter()?.name }}</strong> printerini POS tizimidan olib tashlamoqchimisiz?</p>
              <div class="info-alert">
                ℹ️ Bu amal faqat POS konfiguratsiyasidan printerni o'chiradi. Windows'dagi haqiqiy printer o'chirilmaydi.
              </div>
              @if (deletingPrinter()?.assignedKitchenName) {
                <div class="warning-alert">
                  ⚠️ Bu printer hozir <strong>{{ deletingPrinter()?.assignedKitchenName }}</strong> oshxonasiga biriktirilgan.
                </div>
              }
            </div>
            <div class="modal-footer">
              <button class="pos-btn pos-btn-secondary" (click)="cancelDeletePrinter()">Bekor qilish</button>
              <button class="pos-btn pos-btn-danger" (click)="executeDeletePrinter()">O'chirish</button>
            </div>
          </div>
        </div>
      }

      <!-- EDIT KITCHEN MODAL -->
      @if (editingKitchen()) {
        <div class="modal-backdrop">
          <div class="pos-modal kitchen-modal fade-in">
            <div class="modal-header">
              <h3>Oshxona bo'limi: {{ editingKitchen()?.name }}</h3>
              <button class="modal-close" (click)="closeKitchenModal()">✕</button>
            </div>
            <div class="modal-body">
              <div class="form-grid">
                <div class="form-group">
                  <label>Nomi *</label>
                  <input type="text" class="pos-input" [(ngModel)]="kitchenForm.name">
                </div>
                <div class="form-group">
                  <label>Kodi *</label>
                  <input type="text" class="pos-input" [(ngModel)]="kitchenForm.code">
                </div>
                <div class="form-group">
                  <label>Bo'lim rangi</label>
                  <input type="color" class="pos-color-input" [(ngModel)]="kitchenForm.color">
                </div>
                <div class="form-group">
                  <label>Biriktirilgan Printer</label>
                  <select class="pos-select" [(ngModel)]="kitchenForm.printerId">
                    <option [ngValue]="null">-- Printerni tanlang --</option>
                    @for (p of printers(); track p.id) {
                      <option [ngValue]="p.id">{{ p.name }} ({{ p.connectionType }})</option>
                    }
                  </select>
                </div>
                <div class="form-group">
                  <label>O'rtacha tayyorlash vaqti (daqiqa)</label>
                  <input type="number" class="pos-input" [(ngModel)]="kitchenForm.preparationTimeMinutes" min="1" max="120">
                </div>
                <div class="form-group span-2">
                  <div class="toggle-list">
                    <label class="toggle-item">
                      <input type="checkbox" [(ngModel)]="kitchenForm.autoPrint">
                      <span>Avtomatik chek chiqarish (Auto Print)</span>
                    </label>
                    <label class="toggle-item">
                      <input type="checkbox" [(ngModel)]="kitchenForm.soundNotification">
                      <span>Yangi buyurtmada ovozli signal berish</span>
                    </label>
                  </div>
                </div>
              </div>
            </div>
            <div class="modal-footer">
              <button class="pos-btn pos-btn-secondary" (click)="closeKitchenModal()">Bekor qilish</button>
              <button class="pos-btn pos-btn-primary" (click)="saveKitchen()">Saqlash</button>
            </div>
          </div>
        </div>
      }
    </div>
  `,
  styles: [`
    .settings-container {
      padding: 0 0 32px 0;
      color: var(--text-primary);
    }
    .settings-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: 24px;
      padding-bottom: 16px;
      border-bottom: 1px solid var(--border);
    }
    .settings-title {
      font-size: 24px;
      font-weight: 700;
      margin: 0 0 4px 0;
      color: var(--text-primary);
    }
    .settings-subtitle {
      color: var(--text-muted);
      font-size: 14px;
      margin: 0;
    }
    .header-actions {
      display: flex;
      align-items: center;
      gap: 16px;
    }
    .system-status-pill {
      display: flex;
      align-items: center;
      gap: 8px;
      padding: 6px 12px;
      background: var(--bg-tertiary);
      border: 1px solid var(--border);
      border-radius: 100px;
      font-size: 12px;
      font-weight: 600;
      color: var(--text-secondary);

      &.online {
        border-color: rgba(16, 185, 129, 0.4);
        color: var(--success);
        .status-dot { background: var(--success); box-shadow: 0 0 8px var(--success); }
      }
    }
    .status-dot {
      width: 8px;
      height: 8px;
      border-radius: 50%;
      background: var(--text-muted);
    }

    .toast-banner {
      display: flex;
      align-items: center;
      justify-content: space-between;
      padding: 12px 16px;
      border-radius: var(--radius-md);
      margin-bottom: 20px;
      font-size: 14px;
      font-weight: 500;

      &.success {
        background: rgba(16, 185, 129, 0.15);
        border: 1px solid var(--success);
        color: #34d399;
      }
      &.error {
        background: rgba(239, 68, 68, 0.15);
        border: 1px solid var(--danger);
        color: #f87171;
      }
    }
    .toast-close {
      background: none;
      border: none;
      color: inherit;
      cursor: pointer;
      font-size: 16px;
    }

    .settings-layout {
      display: grid;
      grid-template-columns: 260px 1fr;
      gap: 24px;
      align-items: start;
    }

    /* LEFT SIDEBAR */
    .settings-nav {
      background: var(--bg-card);
      border: 1px solid var(--border);
      border-radius: var(--radius-lg);
      padding: 12px;
      display: flex;
      flex-direction: column;
      gap: 4px;
    }
    .nav-group-title {
      font-size: 11px;
      font-weight: 700;
      color: var(--text-muted);
      letter-spacing: 0.5px;
      padding: 12px 8px 4px 8px;
    }
    .nav-item {
      display: flex;
      align-items: center;
      gap: 10px;
      width: 100%;
      padding: 10px 12px;
      border: none;
      background: transparent;
      color: var(--text-secondary);
      border-radius: var(--radius-sm);
      font-size: 13px;
      font-weight: 500;
      cursor: pointer;
      text-align: left;
      transition: all var(--transition);

      &:hover {
        background: var(--bg-hover);
        color: var(--text-primary);
      }
      &.active {
        background: rgba(99, 102, 241, 0.15);
        color: var(--primary-light);
        font-weight: 600;
      }
      &.highlight {
        border: 1px dashed rgba(99, 102, 241, 0.3);
      }
    }
    .nav-icon { font-size: 16px; }
    .nav-label { flex: 1; }
    .nav-badge {
      background: var(--bg-tertiary);
      color: var(--text-muted);
      font-size: 11px;
      font-weight: 700;
      padding: 2px 6px;
      border-radius: 100px;

      &.printer-badge {
        background: rgba(99, 102, 241, 0.2);
        color: var(--primary-light);
      }
    }

    /* RIGHT CONTENT */
    .settings-content {
      min-width: 0;
    }
    .category-card {
      background: var(--bg-card);
      border: 1px solid var(--border);
      border-radius: var(--radius-lg);
      padding: 24px;
    }
    .card-header {
      margin-bottom: 24px;
      h3 { font-size: 18px; margin: 0 0 4px 0; }
      p { font-size: 13px; color: var(--text-muted); margin: 0; }
    }
    .card-header-flex {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: 24px;
      h3 { font-size: 18px; margin: 0 0 4px 0; }
      p { font-size: 13px; color: var(--text-muted); margin: 0; }
    }

    /* FORMS */
    .form-grid {
      display: grid;
      grid-template-columns: repeat(2, 1fr);
      gap: 16px;
    }
    .span-2 { grid-column: span 2; }
    .form-group {
      display: flex;
      flex-direction: column;
      gap: 6px;
      label { font-size: 13px; font-weight: 600; color: var(--text-secondary); }
    }
    .pos-input, .pos-select, .pos-textarea {
      background: var(--bg-primary);
      border: 1px solid var(--border);
      color: var(--text-primary);
      padding: 10px 12px;
      border-radius: var(--radius-sm);
      font-size: 14px;
      width: 100%;
      outline: none;
      transition: border var(--transition);

      &:focus { border-color: var(--primary); }
    }
    .pos-textarea { resize: vertical; }
    .pos-color-input {
      width: 50px;
      height: 38px;
      padding: 0;
      border: 1px solid var(--border);
      border-radius: var(--radius-sm);
      cursor: pointer;
      background: transparent;
    }
    .input-with-addon {
      display: flex;
      align-items: center;
      .pos-input { border-top-right-radius: 0; border-bottom-right-radius: 0; }
      .addon {
        background: var(--bg-tertiary);
        border: 1px solid var(--border);
        border-left: none;
        padding: 10px 14px;
        border-top-right-radius: var(--radius-sm);
        border-bottom-right-radius: var(--radius-sm);
        font-size: 13px;
        color: var(--text-muted);
      }
    }
    .muted-small { font-size: 12px; color: var(--text-muted); }
    .code-font { font-family: var(--font-mono); }

    .toggle-list {
      display: flex;
      flex-direction: column;
      gap: 12px;
      background: var(--bg-primary);
      border: 1px solid var(--border);
      border-radius: var(--radius-md);
      padding: 16px;
    }
    .toggle-item {
      display: flex;
      align-items: flex-start;
      gap: 12px;
      cursor: pointer;
      user-select: none;
      input[type="checkbox"] {
        width: 18px;
        height: 18px;
        margin-top: 2px;
        accent-color: var(--primary);
      }
      strong { font-size: 14px; display: block; color: var(--text-primary); }
      p { font-size: 12px; color: var(--text-muted); margin: 2px 0 0 0; }
    }

    /* STATS ROW */
    .stats-cards-row {
      display: grid;
      grid-template-columns: repeat(4, 1fr);
      gap: 12px;
      margin-bottom: 24px;
    }
    .stat-box {
      background: var(--bg-primary);
      border: 1px solid var(--border);
      border-radius: var(--radius-md);
      padding: 16px;
      text-align: center;
      display: flex;
      flex-direction: column;
      gap: 4px;
      .stat-val { font-size: 20px; font-weight: 700; color: var(--text-primary); }
      .stat-lbl { font-size: 12px; color: var(--text-muted); }

      &.success .stat-val { color: var(--success); }
      &.danger .stat-val { color: var(--danger); }
    }

    /* PRINTERS GRID */
    .printers-grid {
      display: grid;
      grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
      gap: 16px;
    }
    .printer-card {
      background: var(--bg-primary);
      border: 1px solid var(--border);
      border-radius: var(--radius-md);
      padding: 18px;
      display: flex;
      flex-direction: column;
      gap: 14px;
      transition: all var(--transition);

      &:hover {
        border-color: var(--border-light);
        box-shadow: var(--shadow-md);
      }
      &.is-default {
        border-color: rgba(99, 102, 241, 0.5);
      }
    }
    .printer-card-top {
      display: flex;
      justify-content: space-between;
      align-items: flex-start;
    }
    .printer-name {
      font-size: 16px;
      font-weight: 700;
      color: var(--text-primary);
      display: flex;
      align-items: center;
      gap: 6px;
    }
    .default-badge {
      font-size: 10px;
      font-weight: 800;
      background: var(--primary);
      color: white;
      padding: 1px 6px;
      border-radius: 4px;
    }
    .printer-model {
      font-size: 12px;
      color: var(--text-muted);
      margin-top: 2px;
    }
    .status-indicator {
      font-size: 12px;
      font-weight: 700;
      padding: 3px 8px;
      border-radius: 100px;
      &.online { background: rgba(16, 185, 129, 0.15); color: var(--success); }
      &.offline { background: rgba(239, 68, 68, 0.15); color: var(--danger); }
      &.notfound { background: rgba(249, 115, 22, 0.15); color: #fb923c; }
      &.unknown { background: rgba(156, 163, 175, 0.15); color: #9ca3af; }
    }
    .action-btn-group {
      display: flex;
      gap: 10px;
      align-items: center;
    }
    .discovery-section {
      background: var(--bg-tertiary);
      border: 1px solid var(--border);
      border-radius: var(--radius-md);
      padding: 14px;
      margin-bottom: 16px;
    }
    .discovery-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: 12px;
      .section-title {
        font-weight: 700;
        font-size: 13px;
        color: var(--text-primary);
      }
    }
    .discovery-loader {
      display: flex;
      align-items: center;
      gap: 8px;
      padding: 16px;
      justify-content: center;
      color: var(--text-muted);
      font-size: 13px;
    }
    .empty-printers-alert {
      background: rgba(245, 158, 11, 0.12);
      border: 1px solid rgba(245, 158, 11, 0.3);
      padding: 12px;
      border-radius: var(--radius-sm);
      font-size: 13px;
      color: #fbbf24;
    }
    .windows-printers-list {
      display: flex;
      flex-direction: column;
      gap: 8px;
      max-height: 220px;
      overflow-y: auto;
    }
    .win-printer-card {
      display: flex;
      justify-content: space-between;
      align-items: center;
      padding: 10px 14px;
      background: var(--bg-primary);
      border: 1px solid var(--border);
      border-radius: var(--radius-sm);
      cursor: pointer;
      transition: all var(--transition);

      &:hover {
        border-color: var(--primary);
        background: rgba(99, 102, 241, 0.05);
      }
      &.selected {
        border-color: var(--primary);
        background: rgba(99, 102, 241, 0.12);
        box-shadow: 0 0 0 1px var(--primary);
      }
    }
    .win-printer-left {
      display: flex;
      align-items: center;
      gap: 10px;
      .radio-circle {
        font-size: 16px;
        color: var(--primary);
      }
    }
    .win-printer-name {
      font-weight: 600;
      font-size: 13px;
      color: var(--text-primary);
      display: flex;
      align-items: center;
      gap: 6px;
    }
    .win-printer-driver {
      font-size: 11px;
      color: var(--text-muted);
    }
    .selected-printer-pill {
      display: flex;
      align-items: center;
      gap: 8px;
      padding: 10px 14px;
      background: rgba(99, 102, 241, 0.12);
      border: 1px solid rgba(99, 102, 241, 0.3);
      border-radius: var(--radius-sm);
      margin-bottom: 16px;
      font-size: 13px;
      color: var(--text-primary);
      strong {
        color: var(--primary-light);
      }
    }
    .config-divider {
      height: 1px;
      background: var(--border);
      margin: 16px 0;
    }
    .info-alert {
      background: rgba(99, 102, 241, 0.12);
      border: 1px solid rgba(99, 102, 241, 0.3);
      padding: 10px 14px;
      border-radius: var(--radius-sm);
      font-size: 13px;
      color: var(--text-primary);
      margin: 12px 0;
    }
    .printer-details {
      display: flex;
      flex-direction: column;
      gap: 6px;
      font-size: 13px;
    }
    .detail-row {
      display: flex;
      justify-content: space-between;
      align-items: center;
      .lbl { color: var(--text-muted); }
      .val { color: var(--text-primary); font-weight: 500; }
    }
    .badge-conn {
      background: var(--bg-tertiary);
      padding: 2px 6px;
      border-radius: 4px;
      font-size: 11px;
      font-weight: 700;
    }
    .purpose-tag {
      font-size: 11px;
      font-weight: 700;
      padding: 2px 6px;
      border-radius: 4px;
      &.kitchen { background: rgba(245, 158, 11, 0.15); color: var(--warning); }
      &.cashier { background: rgba(99, 102, 241, 0.15); color: var(--primary-light); }
    }
    .fallback-tag {
      font-size: 12px;
      color: var(--primary-light);
    }
    .last-error-box {
      background: rgba(239, 68, 68, 0.1);
      border: 1px solid rgba(239, 68, 68, 0.3);
      padding: 8px;
      border-radius: var(--radius-sm);
      font-size: 11px;
      color: #f87171;
    }
    .printer-card-actions {
      display: flex;
      gap: 8px;
      margin-top: auto;
      padding-top: 12px;
      border-top: 1px solid var(--border);
    }

    .test-feedback-box {
      padding: 16px;
      border-radius: var(--radius-md);
      margin-bottom: 20px;
      &.success {
        background: rgba(16, 185, 129, 0.12);
        border: 1px solid var(--success);
        color: #34d399;
      }
      &.error {
        background: rgba(239, 68, 68, 0.12);
        border: 1px solid var(--danger);
        color: #f87171;
      }
    }
    .feedback-header {
      display: flex;
      justify-content: space-between;
      margin-bottom: 8px;
    }
    .feedback-body {
      font-size: 13px;
      display: flex;
      flex-direction: column;
      gap: 4px;
    }
    .error-cause {
      margin-top: 4px;
      padding: 6px;
      background: rgba(0, 0, 0, 0.2);
      border-radius: 4px;
    }

    /* KITCHEN TABLE */
    .pos-table {
      width: 100%;
      border-collapse: collapse;
      font-size: 13px;
      th, td {
        padding: 12px;
        text-align: left;
        border-bottom: 1px solid var(--border);
      }
      th { color: var(--text-muted); font-weight: 600; }
    }
    .color-badge {
      display: inline-block;
      width: 18px;
      height: 18px;
      border-radius: 50%;
      vertical-align: middle;
    }
    .printer-tag {
      font-size: 12px;
      padding: 4px 8px;
      border-radius: 4px;
      background: rgba(239, 68, 68, 0.15);
      color: #f87171;
      &.online {
        background: rgba(16, 185, 129, 0.15);
        color: #34d399;
      }
    }
    .status-pill {
      font-size: 11px;
      font-weight: 700;
      padding: 2px 6px;
      border-radius: 100px;
      background: var(--bg-tertiary);
      color: var(--text-muted);
      &.active {
        background: rgba(16, 185, 129, 0.2);
        color: var(--success);
      }
    }

    /* RECEIPT PREVIEW */
    .receipt-settings-split {
      display: grid;
      grid-template-columns: 1fr 340px;
      gap: 24px;
    }
    .receipt-preview-container {
      background: var(--bg-primary);
      border: 1px solid var(--border);
      border-radius: var(--radius-md);
      padding: 16px;
      text-align: center;
    }
    .preview-label {
      font-size: 11px;
      font-weight: 700;
      color: var(--text-muted);
      letter-spacing: 0.5px;
      margin-bottom: 12px;
    }
    .receipt-paper {
      background: #ffffff;
      color: #111827;
      padding: 20px 16px;
      border-radius: 4px;
      box-shadow: 0 4px 20px rgba(0, 0, 0, 0.4);
      font-family: var(--font-mono);
      font-size: 11px;
      line-height: 1.4;
      text-align: left;
      margin: 0 auto;
      width: 290px;
      &.narrow { width: 230px; font-size: 10px; }
    }
    .receipt-p-center { text-align: center; }
    .bold { font-weight: 700; }
    .small-italic { font-size: 10px; font-style: italic; color: #4b5563; }
    .receipt-divider { text-align: center; overflow: hidden; white-space: nowrap; color: #9ca3af; margin: 4px 0; }
    .receipt-row {
      display: flex;
      justify-content: space-between;
      &.grand-total { font-size: 13px; font-weight: 800; margin-top: 4px; }
    }

    /* ROLES */
    .roles-grid {
      display: grid;
      grid-template-columns: repeat(auto-fill, minmax(240px, 1fr));
      gap: 16px;
    }
    .role-card {
      background: var(--bg-primary);
      border: 1px solid var(--border);
      border-radius: var(--radius-md);
      padding: 18px;
      display: flex;
      flex-direction: column;
      gap: 8px;
      h4 { margin: 0; font-size: 15px; }
      p { font-size: 12px; color: var(--text-muted); margin: 0; }
    }
    .role-badge {
      font-size: 11px;
      font-weight: 800;
      padding: 3px 8px;
      border-radius: 4px;
      width: fit-content;
      background: var(--bg-tertiary);
      color: var(--primary-light);
    }
    .permission-tags {
      display: flex;
      flex-wrap: wrap;
      gap: 4px;
      margin-top: 8px;
    }
    .perm-tag {
      background: var(--bg-tertiary);
      font-size: 10px;
      font-weight: 600;
      padding: 2px 6px;
      border-radius: 3px;
      color: var(--text-secondary);
      &.full { background: rgba(99, 102, 241, 0.2); color: var(--primary-light); }
    }

    /* MODAL */
    .modal-backdrop {
      position: fixed;
      top: 0; left: 0; right: 0; bottom: 0;
      background: rgba(0, 0, 0, 0.7);
      backdrop-filter: blur(4px);
      display: flex;
      align-items: center;
      justify-content: center;
      z-index: 9999;
      padding: 20px;
    }
    .pos-modal {
      background: var(--bg-card);
      border: 1px solid var(--border);
      border-radius: var(--radius-lg);
      width: 100%;
      max-width: 580px;
      max-height: 90vh;
      overflow-y: auto;
      box-shadow: var(--shadow-lg);
    }
    .modal-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      padding: 16px 20px;
      border-bottom: 1px solid var(--border);
      h3 { margin: 0; font-size: 16px; }
    }
    .modal-close {
      background: none;
      border: none;
      color: var(--text-muted);
      cursor: pointer;
      font-size: 18px;
    }
    .modal-body { padding: 20px; }
    .modal-footer {
      padding: 16px 20px;
      border-top: 1px solid var(--border);
      display: flex;
      justify-content: flex-end;
      gap: 12px;
    }

    /* BUTTONS */
    .pos-btn {
      padding: 8px 16px;
      border-radius: var(--radius-sm);
      font-size: 13px;
      font-weight: 600;
      cursor: pointer;
      border: none;
      transition: all var(--transition);
      display: inline-flex;
      align-items: center;
      gap: 6px;
    }
    .pos-btn-primary { background: var(--primary); color: white; &:hover { background: var(--primary-dark); } }
    .pos-btn-secondary { background: var(--bg-tertiary); color: var(--text-primary); &:hover { background: var(--bg-hover); } }
    .pos-btn-danger { background: var(--danger); color: white; &:hover { opacity: 0.9; } }
    .pos-btn-sm {
      padding: 6px 12px;
      border-radius: var(--radius-sm);
      font-size: 12px;
      font-weight: 600;
      cursor: pointer;
      border: 1px solid var(--border);
      background: var(--bg-tertiary);
      color: var(--text-primary);
      transition: all var(--transition);
      &:hover { background: var(--bg-hover); }
      &.btn-test { border-color: rgba(99, 102, 241, 0.4); color: var(--primary-light); }
      &.btn-danger { border-color: rgba(239, 68, 68, 0.4); color: var(--danger-light); }
    }

    .info-alert {
      padding: 16px;
      background: rgba(99, 102, 241, 0.1);
      border: 1px solid rgba(99, 102, 241, 0.3);
      border-radius: var(--radius-md);
      font-size: 13px;
      color: var(--text-primary);
    }
    .warning-alert {
      padding: 12px;
      background: rgba(245, 158, 11, 0.12);
      border: 1px solid var(--warning);
      border-radius: var(--radius-sm);
      font-size: 13px;
      color: #fbbf24;
      margin-top: 12px;
    }
    .sys-details-list {
      display: grid;
      grid-template-columns: repeat(2, 1fr);
      gap: 12px;
      background: var(--bg-primary);
      border: 1px solid var(--border);
      border-radius: var(--radius-md);
      padding: 16px;
      margin-bottom: 24px;
    }
    .sys-item {
      font-size: 13px;
      span { color: var(--text-muted); margin-right: 8px; }
      strong { color: var(--text-primary); }
    }
    .audit-log-section {
      h4 { font-size: 15px; margin: 0 0 12px 0; }
    }
    .action-pill {
      font-size: 10px;
      font-weight: 700;
      padding: 2px 6px;
      border-radius: 4px;
      background: var(--bg-tertiary);
      &.create { background: rgba(16, 185, 129, 0.2); color: var(--success); }
      &.update { background: rgba(99, 102, 241, 0.2); color: var(--primary-light); }
    }
    .spinner {
      width: 12px; height: 12px;
      border: 2px solid rgba(255,255,255,0.3);
      border-top-color: white;
      border-radius: 50%;
      display: inline-block;
      animation: spin 0.6s linear infinite;
    }
    @keyframes spin { to { transform: rotate(360deg); } }
  `]
})
export class SettingsComponent implements OnInit {

  activeCategory = signal<SettingsCategory>('PRINTERS');
  loading = signal(true);
  saving = signal(false);

  // Data signals
  printers = signal<Printer[]>([]);
  kitchens = signal<ExtendedKitchenStation[]>([]);
  sysInfo = signal<SystemInfoDto | null>(null);
  auditLogs = signal<AuditLogEntry[]>([]);

  // Category Models
  restaurant: RestaurantSettings = { name: '', currency: 'UZS', timezone: 'Asia/Tashkent' };
  general: GeneralSettings = { defaultCurrency: 'UZS', dateFormat: 'DD.MM.YYYY', timeFormat: 'HH:mm', language: 'uz', theme: 'dark', autoSave: true, autoRefresh: true, realTimeUpdates: true, confirmBeforeDelete: true, confirmBeforeCancelOrder: true, confirmBeforePayment: true, soundNotifications: true };
  receipt: ReceiptSettings = { header: '', footer: '', showWaiter: true, showCashier: true, showTable: true, showOrderNumber: true, showDateTime: true, showPaymentMethod: true, showDiscount: true, showServiceCharge: true, showTax: false, paperWidth: 80, numberOfCopies: 1 };
  payments: PaymentSettings = { cashEnabled: true, cardEnabled: true, clickEnabled: true, paymeEnabled: true, otherEnabled: false, defaultPaymentMethod: 'CASH', allowMixedPayment: true, requirePaymentConfirmation: false, autoPrintReceiptAfterPayment: true };
  taxService: TaxServiceSettings = { serviceChargeEnabled: true, serviceChargePercent: 10.0, taxEnabled: false, taxPercent: 12.0 };
  orders: OrderSettings = { autoOrderNumber: true, orderNumberPrefix: 'ORD', allowOrderEditing: true, allowItemCancellation: true, allowQuantityEditing: true, requireCancellationReason: true, requireManagerApproval: false, autoSendToKitchen: true, allowSplitBill: true, allowMergeOrders: true, allowReopenOrder: false };
  kitchenDisplay: KitchenSettings = { autoPrintKitchenOrder: true, soundNotificationOnNewTicket: true, autoAcceptOrders: false, ticketFontSize: 'medium', showWaiterOnTicket: true, showTableOnTicket: true, showNotesOnTicket: true };
  notifications: NotificationSettings = { soundEnabled: true, soundVolume: 80, newOrderSound: 'chime', itemReadySound: 'bell', lowStockAlert: true };
  security: SecuritySettings = { requirePinForCashier: true, autoLogoutMinutes: 30, sessionTimeoutMinutes: 120, maxLoginAttempts: 5 };
  backup: BackupSettings = { backupLocation: 'C:/posOybek/backups', autoBackupEnabled: true, backupFrequency: 'DAILY', lastBackupTime: '', dbStatus: '', dbSize: '' };

  // Toast
  toastMessage = signal<string | null>(null);
  toastType = signal<'success' | 'error'>('success');

  // Test Print Result
  testPrintResult = signal<TestPrintResult | null>(null);
  testingId = signal<string | null>(null);

  // Printer Discovery & Modals
  showPrinterModal = signal(false);
  editingPrinter = signal<Printer | null>(null);
  savingPrinter = signal(false);
  deletingPrinter = signal<Printer | null>(null);

  availableWindowsPrinters = signal<AvailablePrinter[]>([]);
  loadingAvailablePrinters = signal(false);
  selectedWindowsPrinter = signal<AvailablePrinter | null>(null);
  refreshingPrinters = signal(false);

  printerForm: any = {
    systemPrinterName: '',
    name: '',
    paperWidth: 80,
    purpose: 'KITCHEN',
    kitchenId: null,
    autoPrint: true,
    isPrimary: true,
    isDefault: false
  };

  // Kitchen Modal
  editingKitchen = signal<ExtendedKitchenStation | null>(null);
  kitchenForm: any = {
    name: '',
    code: '',
    color: '#6366F1',
    printerId: null,
    autoPrint: true,
    soundNotification: true,
    preparationTimeMinutes: 15
  };

  backingUp = signal(false);

  // Computed
  onlinePrintersCount = computed(() => this.printers().filter(p => p.status === 'ONLINE' && p.active).length);
  offlinePrintersCount = computed(() => this.printers().filter(p => p.status !== 'ONLINE' || !p.active).length);
  defaultCashierPrinterName = computed(() => {
    const def = this.printers().find(p => p.isDefault) || this.printers().find(p => p.purpose === 'CASHIER');
    return def ? def.name : 'Belgilanmagan';
  });

  constructor(
    private settingsService: SettingsService,
    private printerService: PrinterService,
    public auth: AuthService
  ) {}

  ngOnInit(): void {
    this.loadAllData();
  }

  loadAllData(): void {
    this.loading.set(true);

    this.settingsService.getAllSettings().subscribe({
      next: res => {
        if (res.success && res.data) {
          const d = res.data;
          this.restaurant = { ...d.restaurant };
          this.general = { ...d.general };
          this.receipt = { ...d.receipt };
          this.payments = { ...d.payments };
          this.taxService = { ...d.taxService };
          this.orders = { ...d.orders };
          this.kitchenDisplay = { ...d.kitchen };
          this.notifications = { ...d.notifications };
          this.security = { ...d.security };
          this.backup = { ...d.backup };
        }
        this.loading.set(false);
      },
      error: () => this.loading.set(false)
    });

    this.loadPrinters();
    this.loadKitchens();
    this.loadSysInfo();
    this.loadAuditLogs();
  }

  loadPrinters(): void {
    this.printerService.getPrinters().subscribe({
      next: res => {
        if (res.success && res.data) {
          this.printers.set(res.data);
        }
      }
    });
  }

  loadKitchens(): void {
    this.printerService.getKitchenStations().subscribe({
      next: res => {
        if (res.success && res.data) {
          this.kitchens.set(res.data);
        }
      }
    });
  }

  loadSysInfo(): void {
    this.settingsService.getSystemInfo().subscribe({
      next: res => {
        if (res.success && res.data) {
          this.sysInfo.set(res.data);
        }
      }
    });
  }

  loadAuditLogs(): void {
    this.settingsService.getAuditLogs(15).subscribe({
      next: res => {
        if (res.success && res.data) {
          this.auditLogs.set(res.data);
        }
      }
    });
  }

  setCategory(cat: SettingsCategory): void {
    this.activeCategory.set(cat);
    this.clearTestResult();
  }

  saveActiveCategory(): void {
    this.saving.set(true);
    const cat = this.activeCategory();

    if (cat === 'RESTAURANT') {
      this.settingsService.updateRestaurantSettings(this.restaurant).subscribe({
        next: res => {
          this.saving.set(false);
          this.showToast('Restoran ma\'lumotlari muvaffaqiyatli saqlandi', 'success');
          this.loadAuditLogs();
        },
        error: err => {
          this.saving.set(false);
          this.showToast(err.error?.message || 'Saqlashda xatolik yuz berdi', 'error');
        }
      });
    } else if (cat === 'GENERAL') {
      this.settingsService.updateCategorySettings('GENERAL', this.general).subscribe(this.handleSaveResponse('Umumiy sozlamalar'));
    } else if (cat === 'RECEIPT') {
      this.settingsService.updateCategorySettings('RECEIPT', this.receipt).subscribe(this.handleSaveResponse('Chek sozlamalari'));
    } else if (cat === 'PAYMENTS') {
      this.settingsService.updateCategorySettings('PAYMENTS', this.payments).subscribe(this.handleSaveResponse('To\'lov sozlamalari'));
    } else if (cat === 'TAX_SERVICE') {
      this.settingsService.updateCategorySettings('TAX_SERVICE', this.taxService).subscribe(this.handleSaveResponse('Soliq va xizmat sozlamalari'));
    } else if (cat === 'ORDERS') {
      this.settingsService.updateCategorySettings('ORDERS', this.orders).subscribe(this.handleSaveResponse('Buyurtma sozlamalari'));
    } else if (cat === 'KITCHEN_DISPLAY') {
      this.settingsService.updateCategorySettings('KITCHEN', this.kitchenDisplay).subscribe(this.handleSaveResponse('Oshxona ekrani sozlamalari'));
    } else if (cat === 'NOTIFICATIONS') {
      this.settingsService.updateCategorySettings('NOTIFICATIONS', this.notifications).subscribe(this.handleSaveResponse('Bildirishnoma sozlamalari'));
    } else if (cat === 'SECURITY') {
      this.settingsService.updateCategorySettings('SECURITY', this.security).subscribe(this.handleSaveResponse('Xavfsizlik sozlamalari'));
    } else if (cat === 'BACKUP') {
      this.settingsService.updateCategorySettings('BACKUP', this.backup).subscribe(this.handleSaveResponse('Zaxira sozlamalari'));
    } else {
      this.saving.set(false);
      this.showToast('Sozlamalar saqlandi', 'success');
    }
  }

  private handleSaveResponse(name: string) {
    return {
      next: () => {
        this.saving.set(false);
        this.showToast(`${name} muvaffaqiyatli saqlandi`, 'success');
        this.loadAuditLogs();
      },
      error: (err: any) => {
        this.saving.set(false);
        this.showToast(err.error?.message || 'Saqlashda xatolik yuz berdi', 'error');
      }
    };
  }

  // --- PRINTER OPERATIONS ---
  testPrintPrinter(p: Printer): void {
    this.testingId.set(p.id);
    this.clearTestResult();

    this.printerService.testPrint(p.id).subscribe({
      next: res => {
        this.testingId.set(null);
        if (res.success && res.data) {
          this.testPrintResult.set(res.data);
          this.loadPrinters(); // update online/offline status
          if (res.data.success) {
            this.showToast(`${p.name}: Test chek muvaffaqiyatli chop etildi`, 'success');
          } else {
            this.showToast(`${p.name}: Ulanish xatosi`, 'error');
          }
        }
      },
      error: err => {
        this.testingId.set(null);
        this.testPrintResult.set({
          success: false,
          message: err.error?.message || 'Server xatosi',
          printerName: p.name,
          connectionType: p.connectionType,
          target: p.ipAddress || p.windowsPrinterName || 'Noma\'lum',
          errorDetails: err.error?.message,
          testedAt: new Date().toISOString()
        });
        this.loadPrinters();
      }
    });
  }

  loadAvailableWindowsPrinters(): void {
    this.loadingAvailablePrinters.set(true);
    this.printerService.getAvailablePrinters().subscribe({
      next: res => {
        this.loadingAvailablePrinters.set(false);
        if (res.success && res.data) {
          this.availableWindowsPrinters.set(res.data);
          if (res.data.length > 0 && !this.selectedWindowsPrinter()) {
            this.selectWindowsPrinter(res.data[0]);
          }
        }
      },
      error: err => {
        this.loadingAvailablePrinters.set(false);
        this.showToast(err.error?.message || 'Windows printerlarini qidirishda xatolik', 'error');
      }
    });
  }

  selectWindowsPrinter(wp: AvailablePrinter): void {
    this.selectedWindowsPrinter.set(wp);
    this.printerForm.systemPrinterName = wp.systemPrinterName;
    if (!this.printerForm.name || this.printerForm.name === this.selectedWindowsPrinter()?.displayName) {
      this.printerForm.name = wp.displayName;
    }
  }

  refreshPrinters(): void {
    this.refreshingPrinters.set(true);
    this.printerService.refreshPrinters().subscribe({
      next: res => {
        this.refreshingPrinters.set(false);
        if (res.success && res.data) {
          this.printers.set(res.data);
          this.showToast('Windows printerlar holati yangilandi', 'success');
        }
      },
      error: err => {
        this.refreshingPrinters.set(false);
        this.showToast(err.error?.message || 'Holatni yangilashda xatolik', 'error');
      }
    });
  }

  openAddPrinterModal(): void {
    this.editingPrinter.set(null);
    this.selectedWindowsPrinter.set(null);
    this.printerForm = {
      systemPrinterName: '',
      name: '',
      paperWidth: 80,
      purpose: 'KITCHEN',
      kitchenId: this.kitchens().length > 0 ? this.kitchens()[0].id : null,
      autoPrint: true,
      isPrimary: true,
      isDefault: false
    };
    this.showPrinterModal.set(true);
    this.loadAvailableWindowsPrinters();
  }

  editPrinter(p: Printer): void {
    this.editingPrinter.set(p);
    this.selectedWindowsPrinter.set(null);
    this.printerForm = {
      systemPrinterName: p.systemPrinterName || p.windowsPrinterName || '',
      name: p.name,
      paperWidth: p.paperWidth || 80,
      purpose: p.purpose,
      kitchenId: p.assignedKitchenId || null,
      autoPrint: p.autoPrint,
      isPrimary: p.isPrimaryForKitchen ?? true,
      isDefault: p.isDefault
    };
    this.showPrinterModal.set(true);
  }

  closePrinterModal(): void {
    this.showPrinterModal.set(false);
    this.editingPrinter.set(null);
    this.selectedWindowsPrinter.set(null);
  }

  savePrinter(): void {
    if (!this.editingPrinter() && !this.printerForm.systemPrinterName) {
      this.showToast('Iltimos, Windows printerini tanlang', 'error');
      return;
    }

    if (!this.printerForm.name || !this.printerForm.name.trim()) {
      this.printerForm.name = this.printerForm.systemPrinterName;
    }

    if (this.printerForm.purpose === 'KITCHEN' && !this.printerForm.kitchenId) {
      this.showToast('Oshxona printeri uchun oshxona bo\'limini tanlash majburiy', 'error');
      return;
    }

    this.savingPrinter.set(true);

    if (this.editingPrinter()) {
      const id = this.editingPrinter()!.id;
      this.printerService.updatePrinter(id, this.printerForm).subscribe({
        next: () => {
          this.savingPrinter.set(false);
          this.closePrinterModal();
          this.loadPrinters();
          this.loadKitchens();
          this.loadAuditLogs();
          this.showToast('Printer muvaffaqiyatli yangilandi', 'success');
        },
        error: err => {
          this.savingPrinter.set(false);
          this.showToast(err.error?.message || 'Printer saqlashda xatolik', 'error');
        }
      });
    } else {
      this.printerService.createPrinter(this.printerForm).subscribe({
        next: () => {
          this.savingPrinter.set(false);
          this.closePrinterModal();
          this.loadPrinters();
          this.loadKitchens();
          this.loadAuditLogs();
          this.showToast('Yangi printer muvaffaqiyatli qo\'shildi', 'success');
        },
        error: err => {
          this.savingPrinter.set(false);
          this.showToast(err.error?.message || 'Printer yaratishda xatolik', 'error');
        }
      });
    }
  }

  confirmDeletePrinter(p: Printer): void {
    this.deletingPrinter.set(p);
  }

  cancelDeletePrinter(): void {
    this.deletingPrinter.set(null);
  }

  executeDeletePrinter(): void {
    const p = this.deletingPrinter();
    if (!p) return;

    this.printerService.deletePrinter(p.id).subscribe({
      next: () => {
        this.deletingPrinter.set(null);
        this.loadPrinters();
        this.loadKitchens();
        this.loadAuditLogs();
        this.showToast('Printer o\'chirildi', 'success');
      },
      error: err => {
        this.deletingPrinter.set(null);
        this.showToast(err.error?.message || 'Printerni o\'chirishda xatolik', 'error');
      }
    });
  }

  // --- KITCHEN OPERATIONS ---
  openAddKitchenModal(): void {
    this.editingKitchen.set({
      id: '',
      name: '',
      code: '',
      sortOrder: this.kitchens().length + 1,
      active: true,
      color: '#6366F1',
      autoPrint: true,
      soundNotification: true,
      preparationTimeMinutes: 15
    });
    this.kitchenForm = {
      name: '',
      code: '',
      color: '#6366F1',
      printerId: null,
      autoPrint: true,
      soundNotification: true,
      preparationTimeMinutes: 15
    };
  }

  editKitchen(k: ExtendedKitchenStation): void {
    this.editingKitchen.set(k);
    this.kitchenForm = {
      name: k.name,
      code: k.code,
      color: k.color || '#6366F1',
      printerId: k.printerId || null,
      autoPrint: k.autoPrint ?? true,
      soundNotification: k.soundNotification ?? true,
      preparationTimeMinutes: k.preparationTimeMinutes || 15
    };
  }

  closeKitchenModal(): void {
    this.editingKitchen.set(null);
  }

  saveKitchen(): void {
    const k = this.editingKitchen();
    if (!k) return;

    if (!this.kitchenForm.name.trim() || !this.kitchenForm.code.trim()) {
      this.showToast('Oshxona nomi va kodi majburiy', 'error');
      return;
    }

    this.printerService.updateKitchenStation(k.id, this.kitchenForm).subscribe({
      next: () => {
        this.closeKitchenModal();
        this.loadKitchens();
        this.loadPrinters();
        this.showToast('Oshxona ma\'lumotlari yangilandi', 'success');
      },
      error: err => {
        this.showToast(err.error?.message || 'Oshxonani yangilashda xatolik', 'error');
      }
    });
  }

  triggerManualBackup(): void {
    this.backingUp.set(true);
    this.settingsService.triggerBackup().subscribe({
      next: res => {
        this.backingUp.set(false);
        this.showToast(res.message || 'Zaxira nusxa olindi', 'success');
        this.backup.lastBackupTime = new Date().toLocaleString();
      },
      error: err => {
        this.backingUp.set(false);
        this.showToast(err.error?.message || 'Zaxiralashda xatolik yuz berdi', 'error');
      }
    });
  }

  clearTestResult(): void {
    this.testPrintResult.set(null);
  }

  showToast(msg: string, type: 'success' | 'error'): void {
    this.toastMessage.set(msg);
    this.toastType.set(type);
    setTimeout(() => {
      if (this.toastMessage() === msg) {
        this.clearToast();
      }
    }, 5000);
  }

  clearToast(): void {
    this.toastMessage.set(null);
  }
}
