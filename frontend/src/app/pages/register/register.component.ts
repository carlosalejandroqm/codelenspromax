import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../../core/auth/auth.service';
import { describeHttpError } from '../../core/http-error';

@Component({
  selector: 'cl-register',
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
        <h1 class="auth__title">Crear cuenta</h1>
        <p class="auth__lead">Cada usuario ve solo su propio historial de analisis.</p>

        @if (error()) {
          <div class="alert alert--error" role="alert">{{ error() }}</div>
        }

        <label class="field">
          <span class="field__label">Nombre completo</span>
          <input class="input" formControlName="fullName" autocomplete="name" autofocus />
        </label>

        <label class="field">
          <span class="field__label">Usuario</span>
          <input class="input" formControlName="username" autocomplete="username" />
          @if (form.controls.username.touched && form.controls.username.invalid) {
            <span class="field__error">Entre 3 y 50 caracteres: letras, numeros, punto, guion o guion bajo.</span>
          }
        </label>

        <label class="field">
          <span class="field__label">Correo</span>
          <input class="input" type="email" formControlName="email" autocomplete="email" />
          @if (form.controls.email.touched && form.controls.email.invalid) {
            <span class="field__error">Escribe un correo valido.</span>
          }
        </label>

        <label class="field">
          <span class="field__label">Contrasena</span>
          <input class="input" type="password" formControlName="password" autocomplete="new-password" />
          <span class="field__hint">Minimo 6 caracteres.</span>
        </label>

        <button class="btn auth__submit" type="submit" [disabled]="form.invalid || loading()">
          {{ loading() ? 'Creando cuenta...' : 'Crear cuenta' }}
        </button>

        <p class="auth__alt">Ya tienes cuenta? <a routerLink="/login">Inicia sesion</a></p>
      </form>
    </div>
  `,
  styleUrl: '../auth-layout.scss',
})
export class RegisterComponent {
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);

  readonly loading = signal(false);
  readonly error = signal<string | null>(null);

  readonly form = inject(FormBuilder).nonNullable.group({
    fullName: ['', [Validators.required, Validators.maxLength(120)]],
    username: ['', [Validators.required, Validators.minLength(3), Validators.maxLength(50), Validators.pattern(/^[a-zA-Z0-9._-]+$/)]],
    email: ['', [Validators.required, Validators.email]],
    password: ['', [Validators.required, Validators.minLength(6), Validators.maxLength(72)]],
  });

  submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.loading.set(true);
    this.error.set(null);
    this.auth.register(this.form.getRawValue()).subscribe({
      next: () => this.router.navigateByUrl('/'),
      error: (error) => {
        this.error.set(describeHttpError(error));
        this.loading.set(false);
      },
    });
  }
}
