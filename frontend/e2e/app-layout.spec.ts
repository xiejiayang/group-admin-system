import { expect, test } from '@playwright/test'

test('桌面端仅滚动右侧内容区且侧栏箭头不越界', async ({ page }) => {
  await page.goto('/')
  await page.getByPlaceholder('请输入账号').fill('superadmin')
  await page.getByPlaceholder('请输入密码').fill('xjyadmin')
  await page.getByRole('button', { name: '登录' }).click()
  await expect(page.getByRole('heading', { name: '任免看板' })).toBeVisible()

  await page.evaluate(() => {
    const content = document.querySelector('.app-content')
    const filler = document.createElement('div')
    filler.dataset.testid = 'scroll-filler'
    filler.style.height = '1600px'
    content?.appendChild(filler)
  })

  const beforeScroll = await page.evaluate(() => {
    const header = document.querySelector('.app-header')
    const content = document.querySelector('.app-content')
    const sidebar = document.querySelector('.app-sidebar')
    const toggle = document.querySelector('.sidebar-toggle')

    if (!header || !content || !sidebar || !toggle) {
      throw new Error('布局元素不存在')
    }

    return {
      headerTop: header.getBoundingClientRect().top,
      contentScrollTop: content.scrollTop,
      contentOverflowY: getComputedStyle(content).overflowY,
      bodyScrollable: document.documentElement.scrollHeight > document.documentElement.clientHeight,
      sidebarRight: sidebar.getBoundingClientRect().right,
      toggleRight: toggle.getBoundingClientRect().right
    }
  })

  await page.locator('.app-content').hover()
  await page.mouse.wheel(0, 900)
  await page.waitForTimeout(100)

  const afterScroll = await page.evaluate(() => {
    const header = document.querySelector('.app-header')
    const content = document.querySelector('.app-content')

    if (!header || !content) {
      throw new Error('布局元素不存在')
    }

    return {
      headerTop: header.getBoundingClientRect().top,
      contentScrollTop: content.scrollTop
    }
  })

  expect(beforeScroll.contentOverflowY).toBe('auto')
  expect(beforeScroll.bodyScrollable).toBe(false)
  expect(beforeScroll.toggleRight).toBeLessThanOrEqual(beforeScroll.sidebarRight)
  expect(afterScroll.contentScrollTop).toBeGreaterThan(beforeScroll.contentScrollTop)
  expect(afterScroll.headerTop).toBe(beforeScroll.headerTop)
})
