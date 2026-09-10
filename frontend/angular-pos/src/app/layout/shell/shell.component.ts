import { Component, computed } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { SidebarComponent } from '../sidebar/sidebar.component';
import { TopbarComponent } from '../topbar/topbar.component';
import { ToastContainerComponent } from '../../shared/components/toast-container.component';
import { LoadingService } from '../../core/services/loading.service';

@Component({
  selector: 'app-shell',
  standalone: true,
  imports: [RouterOutlet, SidebarComponent, TopbarComponent, ToastContainerComponent],
  template: `
    @if (loading.isLoading()) {
      <div class="global-loading"></div>
    }

    <div class="pos-layout">
      <app-sidebar (collapsedChange)="sidebarCollapsed = $event" />

      <div class="pos-content" [class.sidebar-collapsed]="sidebarCollapsed">
        <app-topbar />
        <main class="pos-page">
          <router-outlet />
        </main>
      </div>
    </div>

    <app-toast-container />
  `,
  styles: [`
    :host { display: block; height: 100vh; }
  `]
})
export class ShellComponent {
  sidebarCollapsed = false;

  constructor(public loading: LoadingService) {}
}
