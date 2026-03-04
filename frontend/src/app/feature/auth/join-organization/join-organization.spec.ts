import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { JoinOrganizationComponent } from './join-organization.component';

describe('JoinOrganization', () => {
  let component: JoinOrganizationComponent;
  let fixture: ComponentFixture<JoinOrganizationComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [JoinOrganizationComponent],
      providers: [
        provideRouter([])  // ← c'est tout ce qu'il manquait
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(JoinOrganizationComponent);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});