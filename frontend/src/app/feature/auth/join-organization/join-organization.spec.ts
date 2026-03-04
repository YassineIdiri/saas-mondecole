import { ComponentFixture, TestBed } from '@angular/core/testing';

import { JoinOrganization } from './join-organization';

describe('JoinOrganization', () => {
  let component: JoinOrganization;
  let fixture: ComponentFixture<JoinOrganization>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [JoinOrganization]
    })
    .compileComponents();

    fixture = TestBed.createComponent(JoinOrganization);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
