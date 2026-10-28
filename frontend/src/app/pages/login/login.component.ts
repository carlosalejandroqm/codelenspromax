import { ChangeDetectionStrategy, Component, inject, input, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../../core/auth/auth.service';
import { describeHttpError } from '../../core/http-error';

@Component({
  selector: 'cl-login',
  standalone: true,
  imports: [ReactiveFormsModule, RouterLink],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="auth">
      <div class="auth__brand">
        <span class="auth__mark" aria-hidden="true">
          <svg viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round"><circle cx="10.5" cy="10.5" r="6.5" /><path d="m20 20-4.8-4.8" /></svg>
        </span>
        CodeLens
      </div>

      <form class="card" [formGroup]="form" (ngSubmit)="submit()" novalidate>
        <h1 class="auth__title">Iniciar sesion</h1>
        <p class="auth__lead">Analiza tu codigo y revisa tus reportes.</p>

        @if (expired()) {
          <div class="alert" role="status">Tu sesion expiro. Vuelve a iniciar sesion.</div>
        }
        @if (error()) {
          <div class="alert alert--error" role="alert">{{ error() }}</div>
        }

        <label class="field">
          <span class="field__label">Usuario o correo</span>
          <input class="input" formControlName="username" autocomplete="username" autofocus />
        </label>

        <label class="field">
          <span class="field__label">Contrasena</span>
          <input class="input" type="password" formControlName="password" autocomplete="current-password" />
        </label>

        <button class="btn auth__submit" type="submit" [disabled]="form.invalid || loading()">
          {{ loading() ? 'Entrando...' : 'Entrar' }}
        </button>

        <p class="auth__alt">No tienes cuenta? <a routerLink="/register">Crear una</a></p>
      </form>
    </div>
  `,
  styleUrl: '../auth-layout.scss',
})
export class LoginComponent {
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);

  /** Query params enlazados por withComponentInputBinding. */
  readonly returnUrl = input<string>();
  readonly expired = input<string>();

  readonly loading = signal(false);
  readonly error = signal<string | null>(null);

  readonly form = inject(FormBuilder).nonNullable.group({
    username: ['', Validators.required],
    password: ['', Validators.required],
  });

  submit(): void {
    if (this.form.invalid) {
      return;
    }
    this.loading.set(true);
    this.error.set(null);
    this.auth.login(this.form.getRawValue()).subscribe({
      next: () => this.router.navigateByUrl(this.returnUrl() || '/'),
      error: (error) => {
        this.error.set(describeHttpError(error));
        this.loading.set(false);
      },
    });
  }
}
