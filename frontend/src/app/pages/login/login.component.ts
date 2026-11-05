import { Component, OnDestroy, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { AuthService } from '../../core/services/auth.service';

@Component({
  selector: 'app-login',
  templateUrl: './login.component.html',
  styleUrls: ['./login.component.scss']
})
export class LoginComponent implements OnInit, OnDestroy {

  model = { email: '', password: '' };
  loading = false;
  error = '';
  showPassword = false;

  constructor(
    private auth: AuthService,
    private router: Router,
    private route: ActivatedRoute
  ) {}

  ngOnInit(): void {
    document.body.classList.add('auth-blue');
  }

  ngOnDestroy(): void {
    document.body.classList.remove('auth-blue');
  }

  submit(): void {
    if (this.loading) { return; }
    this.loading = true;
    this.error = '';

    this.auth.login(this.model).subscribe({
      next: () => {
        const returnUrl = this.route.snapshot.queryParamMap.get('returnUrl');
        this.router.navigateByUrl(returnUrl && returnUrl.startsWith('/') ? returnUrl : '/dashboard');
      },
      error: (err: Error) => {
        this.error = err.message;
        this.loading = false;
      }
    });
  }
}