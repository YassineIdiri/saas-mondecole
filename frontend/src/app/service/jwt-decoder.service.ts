import { Injectable } from '@angular/core';

export interface JwtPayload {
  sub: string;           // username
  userId: number;
  organizationId: number;
  role: string;          // ADMIN, TEACHER, STUDENT
  iat: number;           // issued at
  exp: number;           // expiration
}

@Injectable({
  providedIn: 'root'
})
export class JwtDecoderService {

  /**
   * Decode JWT token
   */
  decode(token: string): JwtPayload | null {
    if (!token) {
      console.log('❌ JWT Decoder: No token provided'); // ✅ DEBUG
      return null;
    }

    try {
      const parts = token.split('.');
      if (parts.length !== 3) {
        console.log('❌ JWT Decoder: Invalid token format'); // ✅ DEBUG
        return null;
      }

      const payload = parts[1];
      const decoded = atob(payload);
      const parsed = JSON.parse(decoded) as JwtPayload;

      const payloadd = JSON.parse(atob(token.split('.')[1]));
      console.log('Token info:');
      console.log('exp:', new Date(payloadd.exp * 1000));

      console.log('✅ JWT Decoder: Token decoded successfully', parsed); // ✅ DEBUG
      return parsed;
    } catch (e) {
      console.error('❌ JWT Decoder: Error decoding JWT:', e); // ✅ DEBUG
      return null;
    }
  }

  /**
   * Check if token is expired
   */
  isExpired(token: string): boolean {
    const payload = this.decode(token);
    if (!payload) {
      console.log('❌ JWT Decoder: Token is expired (no payload)'); // ✅ DEBUG
      return true;
    }

    const now = Date.now() / 1000; // Convert to seconds
    const isExpired = payload.exp < now;

    console.log('🕐 JWT Decoder: Token expiration check', {
      exp: new Date(payload.exp * 1000),
      now: new Date(now * 1000),
      isExpired
    }); // ✅ DEBUG

    return isExpired;
  }

  /**
   * Get time until expiration in milliseconds
   */
  getTimeUntilExpiration(token: string): number {
    const payload = this.decode(token);
    if (!payload) return 0;

    const now = Date.now() / 1000;
    const timeLeft = payload.exp - now;
    return Math.max(0, timeLeft * 1000); // Convert to milliseconds
  }
}
