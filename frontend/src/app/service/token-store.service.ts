import { Injectable } from '@angular/core';

@Injectable({
  providedIn: 'root'
})
export class TokenStore {
  private readonly TOKEN_KEY = 'accessToken';
  get(): string | null {
    return localStorage.getItem(this.TOKEN_KEY);
  }

  set(token: string): void {
    localStorage.setItem(this.TOKEN_KEY, token);
  }

  clear(): void {
    localStorage.removeItem(this.TOKEN_KEY);
  }

  has(): boolean {
    return !!this.get();
  }
}
