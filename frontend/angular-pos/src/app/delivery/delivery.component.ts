import { Component, OnInit, OnDestroy, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatPaginatorModule, PageEvent } from '@angular/material/paginator';
import { DeliveryService } from './services/delivery.service';
import {
  DeliveryProvider,
  SaveProviderRequest,
  DeliveryOrder,
  DeliveryOrderItem,
  ProductMapping,
  CategoryMapping,
  DeliveryDashboardMetrics,
  DeliveryIntegrationLog,
  ConnectionTestResult
} from './models/delivery.models';
import { ProductService, Product } from '../core/services/product.service';
import { CategoryService, Category } from '../core/services/category.service';
import { NotificationService } from '../core/services/notification.service';
import { AuthService } from '../core/services/auth.service';
import { WebsocketService } from '../core/services/websocket.service';

@Component({
  selector: 'app-delivery',
  standalone: true,
  imports: [CommonModule, FormsModule, MatPaginatorModule],
  templateUrl: './delivery.component.html',
  styleUrls: ['./delivery.component.scss']
})
export class DeliveryComponent implements OnInit, OnDestroy {
  activeTab: 'orders' | 'providers' | 'product-mapping' | 'category-mapping' | 'dashboard' | 'logs' = 'orders';

  loading = false;
  providers: DeliveryProvider[] = [];
  orders: DeliveryOrder[] = [];
  productMappings: ProductMapping[] = [];
  categoryMappings: CategoryMapping[] = [];
  dashboardMetrics: DeliveryDashboardMetrics | null = null;
  logs: DeliveryIntegrationLog[] = [];

  // POS products & categories for mapping dropdowns
  posProducts: Product[] = [];
  posCategories: Category[] = [];

  // Filters & Pagination for Orders
  selectedProviderFilter: string = '';
  selectedStatusFilter: string = '';
  orderSearchQuery: string = '';
  ordersPage = 0;
  ordersSize = 25;
  totalOrders = 0;

  // Logs pagination
  logsPage = 0;
  logsSize = 25;
  totalLogs = 0;

  // Modals & Drawers
  showProviderModal = false;
  isEditingProvider = false;
  selectedProviderId: string | null = null;
  providerForm: SaveProviderRequest = this.initProviderForm();

  showOrderDetailsModal = false;
  selectedOrder: DeliveryOrder | null = null;

  showAcceptModal = false;
  orderToAccept: DeliveryOrder | null = null;
  prepMinutes = 20;

  showRejectModal = false;
  orderToReject: DeliveryOrder | null = null;
  rejectReason = 'Restoran band';

  showCancelModal = false;
  orderToCancel: DeliveryOrder | null = null;
  cancelReason = 'Mijoz talabi bilan bekor qilindi';

  showMapItemModal = false;
  orderToMap: DeliveryOrder | null = null;
  itemToMap: DeliveryOrderItem | null = null;
  selectedPosProductId: string = '';

  showCategoryMapModal = false;
  categoryMapForm = {
    providerId: '',
    externalCategoryId: '',
    externalCategoryName: '',
    posCategoryId: ''
  };

  testingConnectionId: string | null = null;
  connectionTestResult: { [providerId: string]: ConnectionTestResult } = {};

  private wsUnsub?: () => void;

  constructor(
    private deliveryService: DeliveryService,
    private productService: ProductService,
    private categoryService: CategoryService,
    public auth: AuthService,
    private notify: NotificationService,
    private ws: WebsocketService,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    this.loadProviders();
    this.loadOrders();
    this.loadPosMetadata();

    // Subscribe to real-time delivery notifications
    this.wsUnsub = this.ws.subscribe('/topic/delivery', (data: any) => {
      this.notify.info(`🚚 Yangi delivery buyurtma keldi!`);
      this.loadOrders();
      if (this.activeTab === 'dashboard') {
        this.loadDashboard();
      }
      this.cdr.markForCheck();
    });
  }

  ngOnDestroy(): void {
    if (this.wsUnsub) {
      this.wsUnsub();
    }
  }

  setTab(tab: 'orders' | 'providers' | 'product-mapping' | 'category-mapping' | 'dashboard' | 'logs'): void {
    this.activeTab = tab;
    if (tab === 'orders') {
      this.loadOrders();
    } else if (tab === 'providers') {
      this.loadProviders();
    } else if (tab === 'product-mapping') {
      this.loadProductMappings();
    } else if (tab === 'category-mapping') {
      this.loadCategoryMappings();
    } else if (tab === 'dashboard') {
      this.loadDashboard();
    } else if (tab === 'logs') {
      this.loadLogs();
    }
  }

  // ===================== PROVIDERS =====================

  loadProviders(): void {
    this.deliveryService.getProviders().subscribe({
      next: res => {
        if (res.success && res.data) {
          this.providers = res.data;
        }
      },
      error: err => {
        this.notify.error('Xizmatlarni yuklashda xatolik: ' + (err.error?.message || err.message));
      }
    });
  }

  openCreateProviderModal(): void {
    this.isEditingProvider = false;
    this.selectedProviderId = null;
    this.providerForm = this.initProviderForm();
    this.showProviderModal = true;
  }

  openEditProviderModal(provider: DeliveryProvider): void {
    this.isEditingProvider = true;
    this.selectedProviderId = provider.id;
    this.providerForm = {
      name: provider.name,
      code: provider.code,
      type: provider.type,
      status: provider.status,
      apiBaseUrl: provider.apiBaseUrl,
      restaurantId: provider.restaurantId,
      autoAcceptOrders: provider.autoAcceptOrders,
      autoPrintKitchenReceipt: provider.autoPrintKitchenReceipt,
      autoPrintCustomerReceipt: provider.autoPrintCustomerReceipt,
      soundNotification: provider.soundNotification,
      autoSync: provider.autoSync,
      syncIntervalMinutes: provider.syncIntervalMinutes,
      orderTimeoutMinutes: provider.orderTimeoutMinutes,
      defaultPaymentType: provider.defaultPaymentType,
      commissionType: provider.commissionType,
      commissionValue: provider.commissionValue
    };
    this.showProviderModal = true;
  }

  saveProvider(): void {
    if (!this.providerForm.name || !this.providerForm.code || !this.providerForm.type) {
      this.notify.warn('Iltimos, xizmat nomi, kodi va turini to\'ldiring');
      return;
    }

    this.loading = true;
    if (this.isEditingProvider && this.selectedProviderId) {
      this.deliveryService.updateProvider(this.selectedProviderId, this.providerForm).subscribe({
        next: res => {
          this.loading = false;
          this.notify.success('Xizmat muvaffaqiyatli yangilandi');
          this.showProviderModal = false;
          this.loadProviders();
        },
        error: err => {
          this.loading = false;
          this.notify.error('Xatolik: ' + (err.error?.message || err.message));
        }
      });
    } else {
      this.deliveryService.createProvider(this.providerForm).subscribe({
        next: res => {
          this.loading = false;
          this.notify.success('Yangi delivery xizmat muvaffaqiyatli qo\'shildi');
          this.showProviderModal = false;
          this.loadProviders();
        },
        error: err => {
          this.loading = false;
          this.notify.error('Xatolik: ' + (err.error?.message || err.message));
        }
      });
    }
  }

  testConnection(provider: DeliveryProvider): void {
    this.testingConnectionId = provider.id;
    this.deliveryService.testConnection(provider.id).subscribe({
      next: res => {
        this.testingConnectionId = null;
        if (res.success && res.data) {
          this.connectionTestResult[provider.id] = res.data;
          if (res.data.success) {
            this.notify.success(`🟢 ${provider.name}: Ulanish muvaffaqiyatli! (${res.data.latencyMs || 0}ms)`);
          } else {
            this.notify.error(`🔴 ${provider.name}: ${res.data.message}`);
          }
          this.loadProviders();
        }
      },
      error: err => {
        this.testingConnectionId = null;
        this.notify.error(`🔴 Ulanish muvaffaqiyatsiz: ${err.error?.message || 'Server xatosi'}`);
      }
    });
  }

  toggleConnection(provider: DeliveryProvider): void {
    if (provider.connectionStatus === 'CONNECTED') {
      this.deliveryService.disconnectProvider(provider.id).subscribe({
        next: () => {
          this.notify.info(`${provider.name} uzildi (DISCONNECTED)`);
          this.loadProviders();
        },
        error: err => this.notify.error('Xatolik: ' + (err.error?.message || err.message))
      });
    } else {
      this.deliveryService.connectProvider(provider.id).subscribe({
        next: () => {
          this.notify.success(`🟢 ${provider.name} muvaffaqiyatli ulandi (CONNECTED)`);
          this.loadProviders();
        },
        error: err => this.notify.error('Xatolik: ' + (err.error?.message || err.message))
      });
    }
  }

  syncProducts(provider: DeliveryProvider): void {
    this.loading = true;
    this.deliveryService.syncProducts(provider.id).subscribe({
      next: res => {
        this.loading = false;
        this.notify.success(`${provider.name}: Mahsulotlar sinxronizatsiyasi yakunlandi`);
        this.loadProviders();
      },
      error: err => {
        this.loading = false;
        this.notify.error('Sinxronizatsiya xatosi: ' + (err.error?.message || err.message));
      }
    });
  }

  // ===================== ORDERS =====================

  loadOrders(): void {
    this.loading = true;
    this.deliveryService.getOrders({
      providerId: this.selectedProviderFilter || undefined,
      status: this.selectedStatusFilter || undefined,
      search: this.orderSearchQuery || undefined,
      page: this.ordersPage,
      size: this.ordersSize
    }).subscribe({
      next: res => {
        this.loading = false;
        if (res.success && res.data) {
          this.orders = res.data;
          if (res.page) {
            this.totalOrders = res.page.totalElements;
          }
        }
      },
      error: err => {
        this.loading = false;
        this.notify.error('Buyurtmalarni yuklashda xatolik: ' + (err.error?.message || err.message));
      }
    });
  }

  onOrderPageChange(event: PageEvent): void {
    this.ordersPage = event.pageIndex;
    this.ordersSize = event.pageSize;
    this.loadOrders();
  }

  viewOrderDetails(order: DeliveryOrder): void {
    this.selectedOrder = order;
    this.showOrderDetailsModal = true;
  }

  openAcceptModal(order: DeliveryOrder): void {
    this.orderToAccept = order;
    this.prepMinutes = 20;
    this.showAcceptModal = true;
  }

  confirmAcceptOrder(): void {
    if (!this.orderToAccept) return;
    this.deliveryService.acceptOrder(this.orderToAccept.id, this.prepMinutes).subscribe({
      next: res => {
        this.notify.success('Buyurtma qabul qilindi va oshxonaga yuborildi!');
        this.showAcceptModal = false;
        this.orderToAccept = null;
        this.loadOrders();
      },
      error: err => this.notify.error('Xatolik: ' + (err.error?.message || err.message))
    });
  }

  openRejectModal(order: DeliveryOrder): void {
    this.orderToReject = order;
    this.rejectReason = 'Restoran band';
    this.showRejectModal = true;
  }

  confirmRejectOrder(): void {
    if (!this.orderToReject) return;
    this.deliveryService.rejectOrder(this.orderToReject.id, this.rejectReason).subscribe({
      next: res => {
        this.notify.info('Buyurtma rad etildi');
        this.showRejectModal = false;
        this.orderToReject = null;
        this.loadOrders();
      },
      error: err => this.notify.error('Xatolik: ' + (err.error?.message || err.message))
    });
  }

  openCancelModal(order: DeliveryOrder): void {
    this.orderToCancel = order;
    this.cancelReason = 'Mijoz bekor qildi';
    this.showCancelModal = true;
  }

  confirmCancelOrder(): void {
    if (!this.orderToCancel) return;
    this.deliveryService.cancelOrder(this.orderToCancel.id, this.cancelReason).subscribe({
      next: res => {
        this.notify.warn('Buyurtma bekor qilindi');
        this.showCancelModal = false;
        this.orderToCancel = null;
        this.loadOrders();
      },
      error: err => this.notify.error('Xatolik: ' + (err.error?.message || err.message))
    });
  }

  openMapItemModal(order: DeliveryOrder, item: DeliveryOrderItem): void {
    this.orderToMap = order;
    this.itemToMap = item;
    this.selectedPosProductId = '';
    this.showMapItemModal = true;
  }

  confirmMapItem(): void {
    if (!this.orderToMap || !this.itemToMap || !this.selectedPosProductId) {
      this.notify.warn('Iltimos, POS mahsulotini tanlang');
      return;
    }

    this.deliveryService.mapOrderProduct(
      this.orderToMap.id,
      this.itemToMap.externalProductId,
      this.selectedPosProductId
    ).subscribe({
      next: res => {
        this.notify.success('Mahsulot muvaffaqiyatli bog\'landi va buyurtma qayta ishlandi!');
        this.showMapItemModal = false;
        this.orderToMap = null;
        this.itemToMap = null;
        this.loadOrders();
      },
      error: err => this.notify.error('Mapping xatosi: ' + (err.error?.message || err.message))
    });
  }

  // ===================== PRODUCT MAPPINGS =====================

  loadProductMappings(): void {
    this.deliveryService.getProductMappings().subscribe({
      next: res => {
        if (res.success && res.data) {
          this.productMappings = res.data;
        }
      },
      error: err => this.notify.error('Mappinglarni yuklashda xatolik')
    });
  }

  deleteProductMapping(mapping: ProductMapping): void {
    if (confirm(`Haqiqatan ham "${mapping.externalProductName}" mappingini o'chirmoqchimisiz?`)) {
      this.deliveryService.deleteProductMapping(mapping.id).subscribe({
        next: () => {
          this.notify.success('Mapping o\'chirildi');
          this.loadProductMappings();
        },
        error: err => this.notify.error('O\'chirishda xatolik')
      });
    }
  }

  // ===================== CATEGORY MAPPINGS =====================

  loadCategoryMappings(): void {
    this.deliveryService.getCategoryMappings().subscribe({
      next: res => {
        if (res.success && res.data) {
          this.categoryMappings = res.data;
        }
      },
      error: err => this.notify.error('Kategoriya mappinglarini yuklashda xatolik')
    });
  }

  saveCategoryMapping(): void {
    if (!this.categoryMapForm.providerId || !this.categoryMapForm.externalCategoryId || !this.categoryMapForm.posCategoryId) {
      this.notify.warn('Iltimos, barcha maydonlarni to\'ldiring');
      return;
    }

    this.deliveryService.saveCategoryMapping(this.categoryMapForm).subscribe({
      next: () => {
        this.notify.success('Kategoriya mapping saqlandi');
        this.showCategoryMapModal = false;
        this.loadCategoryMappings();
      },
      error: err => this.notify.error('Xatolik: ' + (err.error?.message || err.message))
    });
  }

  deleteCategoryMapping(mapping: CategoryMapping): void {
    if (confirm(`Kategoriya mappingini o'chirmoqchimisiz?`)) {
      this.deliveryService.deleteCategoryMapping(mapping.id).subscribe({
        next: () => {
          this.notify.success('Kategoriya mapping o\'chirildi');
          this.loadCategoryMappings();
        },
        error: err => this.notify.error('O\'chirishda xatolik')
      });
    }
  }

  // ===================== DASHBOARD =====================

  loadDashboard(): void {
    this.deliveryService.getDashboard().subscribe({
      next: res => {
        if (res.success && res.data) {
          this.dashboardMetrics = res.data;
        }
      },
      error: err => this.notify.error('Dashboard statistikasini yuklashda xatolik')
    });
  }

  // ===================== LOGS =====================

  loadLogs(): void {
    this.deliveryService.getLogs(this.logsPage, this.logsSize).subscribe({
      next: res => {
        if (res.success && res.data) {
          this.logs = res.data;
          if (res.page) {
            this.totalLogs = res.page.totalElements;
          }
        }
      },
      error: err => this.notify.error('Loglarni yuklashda xatolik')
    });
  }

  onLogsPageChange(event: PageEvent): void {
    this.logsPage = event.pageIndex;
    this.logsSize = event.pageSize;
    this.loadLogs();
  }

  // ===================== HELPERS =====================

  loadPosMetadata(): void {
    this.productService.getProducts(undefined, undefined, true, 0, 500).subscribe({
      next: res => {
        if (res.success && res.data) {
          this.posProducts = res.data;
        }
      }
    });

    this.categoryService.getCategories(true, undefined, 0, 100).subscribe({
      next: res => {
        if (res.success && res.data) {
          this.posCategories = res.data;
        }
      }
    });
  }

  getProviderIcon(code: string): string {
    const c = (code || '').toUpperCase();
    if (c.includes('YANDEX')) return '🟡';
    if (c.includes('UZUM')) return '🟣';
    if (c.includes('GLOVO')) return '🟡';
    return '🚚';
  }

  getStatusBadgeClass(status: string): string {
    switch (status) {
      case 'NEW': return 'badge--new';
      case 'ACCEPTED':
      case 'PREPARING': return 'badge--progress';
      case 'READY':
      case 'COURIER_ASSIGNED': return 'badge--ready';
      case 'DELIVERED': return 'badge--delivered';
      case 'REJECTED':
      case 'CANCELLED':
      case 'FAILED': return 'badge--danger';
      case 'MAPPING_REQUIRED': return 'badge--warning';
      default: return 'badge--muted';
    }
  }

  getStatusText(status: string): string {
    switch (status) {
      case 'NEW': return 'Yangi';
      case 'ACCEPTED': return 'Qabul qilingan';
      case 'PREPARING': return 'Tayyorlanmoqda';
      case 'READY': return 'Oshxonada tayyor';
      case 'COURIER_ASSIGNED': return 'Kuryer tayinlangan';
      case 'PICKED_UP': return 'Kuryer oldi';
      case 'DELIVERING': return 'Yetkazilmoqda';
      case 'DELIVERED': return 'Yetkazildi';
      case 'REJECTED': return 'Rad etilgan';
      case 'CANCELLED': return 'Bekor qilingan';
      case 'FAILED': return 'Xatolik';
      case 'MAPPING_REQUIRED': return 'Mapping kerak';
      default: return status;
    }
  }

  formatPrice(num?: number): string {
    if (!num) return '0 so\'m';
    return Number(num).toLocaleString('uz-UZ') + ' so\'m';
  }

  formatDate(dateStr?: string): string {
    if (!dateStr) return '-';
    const d = new Date(dateStr);
    return d.toLocaleTimeString('uz-UZ', { hour: '2-digit', minute: '2-digit' }) + ' ' +
           d.toLocaleDateString('uz-UZ', { day: '2-digit', month: '2-digit' });
  }

  private initProviderForm(): SaveProviderRequest {
    return {
      name: '',
      code: '',
      type: 'YANDEX_EATS',
      status: 'ACTIVE',
      apiBaseUrl: '',
      restaurantId: '',
      apiKey: '',
      clientId: '',
      secret: '',
      webhookSecret: '',
      autoAcceptOrders: false,
      autoPrintKitchenReceipt: true,
      autoPrintCustomerReceipt: false,
      soundNotification: true,
      autoSync: true,
      syncIntervalMinutes: 5,
      orderTimeoutMinutes: 30,
      defaultPaymentType: 'ONLINE',
      commissionType: 'PERCENTAGE',
      commissionValue: 15
    };
  }
}
