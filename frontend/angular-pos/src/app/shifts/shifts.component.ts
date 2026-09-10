import { Component } from '@angular/core';

@Component({
  selector: 'app-shifts',
  standalone: true,
  template: `
    <div class="fade-in" style="padding: 0;">
      <div style="margin-bottom: 24px;">
        <h1 style="font-size: 24px; font-weight: 700; color: var(--text-primary); margin-bottom: 4px;">Shift Management</h1>
        <p style="color: var(--text-muted); font-size: 14px;">Module under development — Phase 2+</p>
      </div>
      <div class="pos-card" style="text-align: center; padding: 60px; border-style: dashed;">
        <div style="font-size: 48px; margin-bottom: 16px;">🚧</div>
        <h3 style="color: var(--text-primary); margin-bottom: 8px;">Shift Management Module</h3>
        <p style="color: var(--text-muted);">This module is being implemented. Backend API is ready.</p>
      </div>
    </div>
  `
})
export class ShiftsComponent {}
