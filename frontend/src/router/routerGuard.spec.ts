import { describe, expect, it } from 'vitest'

import { canAccessPath } from './index'

describe('canAccessPath', () => {
  it('allows SUPER_ADMIN to access settings', () => {
    expect(canAccessPath('/settings/users', ['SUPER_ADMIN'])).toBe(true)
  })

  it('blocks regular users from settings', () => {
    expect(canAccessPath('/settings/users', ['GENERAL_ADMIN'], 'GENERAL_ADMIN')).toBe(false)
  })

  it('allows PARTY_HR to access party HR but not general admin', () => {
    expect(canAccessPath('/party-hr', ['PARTY_HR'], 'PARTY_HR')).toBe(true)
    expect(canAccessPath('/general-admin', ['PARTY_HR'], 'PARTY_HR')).toBe(false)
  })

  it('allows GENERAL_ADMIN to access general admin but not party HR', () => {
    expect(canAccessPath('/general-admin', ['GENERAL_ADMIN'], 'GENERAL_ADMIN')).toBe(true)
    expect(canAccessPath('/party-hr', ['GENERAL_ADMIN'], 'GENERAL_ADMIN')).toBe(false)
  })
})
