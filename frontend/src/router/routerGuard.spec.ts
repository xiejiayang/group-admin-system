import { describe, expect, it } from 'vitest'

import type { SystemMenu } from '@/api/system'

import router, { canAccessPath, defaultLandingPath } from './index'

const menu = (path: string, sortOrder: number): SystemMenu => ({
  id: sortOrder,
  name: path,
  path,
  permissionCode: null,
  sortOrder
})

describe('canAccessPath', () => {
  it('allows SUPER_ADMIN to access settings', () => {
    expect(canAccessPath('/settings/users', ['SUPER_ADMIN'], [])).toBe(true)
  })

  it('allows department administrators with settings permission to access settings users and logs', () => {
    expect(canAccessPath('/settings/users', ['PARTY_HR_ADMIN'], ['menu:settings'], 'PARTY_HR')).toBe(true)
    expect(canAccessPath('/settings/logs', ['PARTY_HR_ADMIN'], ['menu:settings'], 'PARTY_HR')).toBe(true)
    expect(canAccessPath('/settings/users', ['GENERAL_ADMIN_ADMIN'], ['menu:settings'], 'GENERAL_ADMIN')).toBe(true)
    expect(canAccessPath('/settings/logs', ['GENERAL_ADMIN_ADMIN'], ['menu:settings'], 'GENERAL_ADMIN')).toBe(true)
  })

  it('blocks department users from settings', () => {
    expect(canAccessPath('/settings/users', ['GENERAL_ADMIN_USER'], ['menu:general-admin'], 'GENERAL_ADMIN')).toBe(false)
    expect(canAccessPath('/settings/logs', ['PARTY_HR_USER'], ['menu:settings'], 'PARTY_HR')).toBe(false)
    expect(canAccessPath('/settings/users', ['PARTY_HR_USER'], ['menu:settings'], 'PARTY_HR')).toBe(false)
  })

  it('blocks PARTY_HR department users without party HR permissions from party HR', () => {
    expect(canAccessPath('/party-hr', ['PARTY_HR_USER'], [], 'PARTY_HR')).toBe(false)
  })

  it('allows PARTY_HR department users with party HR permissions to access party HR but not general admin', () => {
    expect(canAccessPath('/party-hr', ['PARTY_HR_USER'], ['menu:party-hr'], 'PARTY_HR')).toBe(true)
    expect(canAccessPath('/party-hr', ['PARTY_HR_USER'], ['appointment:manage'], 'PARTY_HR')).toBe(true)
    expect(canAccessPath('/general-admin', ['PARTY_HR_USER'], ['menu:party-hr'], 'PARTY_HR')).toBe(false)
  })

  it('allows GENERAL_ADMIN department users with general admin menu permission to access general admin', () => {
    expect(canAccessPath('/general-admin', ['GENERAL_ADMIN_USER'], ['menu:general-admin'], 'GENERAL_ADMIN')).toBe(true)
  })
})

describe('router settings routes', () => {
  it('registers the operation log settings route', () => {
    expect(router.getRoutes().some((route) => route.path === '/settings/logs')).toBe(true)
  })
})

describe('defaultLandingPath', () => {
  it('uses the first backend menu path as the default landing path', () => {
    expect(defaultLandingPath(['GENERAL_ADMIN_USER'], [menu('/general-admin', 1), menu('/party-hr', 2)])).toBe(
      '/general-admin'
    )
  })

  it('falls back to party HR for SUPER_ADMIN without menus', () => {
    expect(defaultLandingPath(['SUPER_ADMIN'], [])).toBe('/party-hr')
  })

  it('sends regular users without menus to forbidden', () => {
    expect(defaultLandingPath(['PARTY_HR_USER'], [])).toBe('/forbidden')
  })
})
