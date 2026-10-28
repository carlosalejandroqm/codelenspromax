import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { AuthService } from '../core/auth/auth.service';

/** Marco de las pantallas autenticadas: cabecera con navegacion y sesion. */
@Component({
  selector: 'cl-shell',
  standalone: true,
  imports: [RouterOutlet, RouterLink, RouterLinkActive],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <header class="top">
      <div class="top__inner">
        <a routerLink="/" class="brand" aria-label="CodeLens, inicio">
          <span class="brand__mark" aria-hidden="true">
            <svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round">
              <circle cx="10.5" cy="10.5" r="6.5" />
              <path d="m20 20-4.8-4.8" />
              <path d="m8.5 9-1.5 1.5L8.5 12M12.5 9l1.5 1.5-1.5 1.5" />
            </svg>
          </span>
          CodeLens
        </a>

        <nav class="nav" aria-label="Principal">
          <a routerLink="/" routerLinkActive="nav__link--active" [routerLinkActiveOptions]="{ exact: true }" class="nav__link">Nuevo analisis</a>
          <a routerLink="/history" routerLinkActive="nav__link--active" class="nav__link">Historial</a>
        </nav>

        <div class="session">
          @if (auth.user(); as user) {
            <span class="session__user" [title]="user.email">
              <span class="session__avatar" aria-hidden="true">{{ initials(user.fullName) }}</span>
              <span class="session__name">{{ user.fullName }}</span>
            </span>
          }
          <button type="button" class="btn btn--ghost btn--sm" (click)="logout()">Salir</button>
        </div>
      </div>
    </header>

    <main class="page">
      <router-outlet />
    </main>
  `,
  styleUrl: './shell.component.scss',
})
export class ShellComponent {
  protected readonly auth = inject(AuthService);
  private readonly router = inject(Router);

  initials(name: string): string {
    return name
      .split(/\s+/)
      .filter(Boolean)
      .slice(0, 2)
      .map((part) => part[0]?.toUpperCase() ?? '')
      .join('');
  }

  logout(): void {
    this.auth.logout();
    this.router.navigate(['/login']);
  }
}
