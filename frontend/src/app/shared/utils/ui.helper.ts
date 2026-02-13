import { ChangeDetectorRef } from '@angular/core';
import { MonoTypeOperatorFunction } from 'rxjs';
import { finalize, tap } from 'rxjs/operators';

export type Ui = {
  repaint: () => void;
  set: (fn: () => void) => void;
  pipeRepaint: <T>() => MonoTypeOperatorFunction<T>;
};

/**
 * Helper pour les apps Angular zoneless :
 * - repaint() force le refresh UI
 * - set(fn) = fait une mutation + repaint
 * - pipeRepaint() = opérateur RxJS qui repaint sur next/error + à la fin
 */
export function ui(cdr: ChangeDetectorRef): Ui {
  const repaint = () => {
    try {
      cdr.detectChanges();
    } catch {
      // ignore (peut arriver si le composant est détruit)
    }
  };

  const set = (fn: () => void) => {
    fn();
    repaint();
  };

  const pipeRepaint = <T>(): MonoTypeOperatorFunction<T> =>
    (source$) =>
      source$.pipe(
        tap({
          next: () => repaint(),
          error: () => repaint(),
        }),
        finalize(() => repaint())
      );

  return { repaint, set, pipeRepaint };
}