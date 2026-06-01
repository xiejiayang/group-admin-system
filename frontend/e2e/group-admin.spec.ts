import { expect, test } from '@playwright/test'

test('superadmin can login and see all menus', async ({ page }) => {
  await page.goto('/login')

  await page.getByPlaceholder('请输入账号').fill('superadmin')
  await page.getByPlaceholder('请输入密码').fill('xjyadmin')
  await page.getByRole('button', { name: /^登录$/ }).click()

  await expect(page).toHaveURL(/\/party-hr/)
  await expect(page.getByRole('heading', { name: '任免看板' })).toBeVisible()

  const menu = page.locator('.permission-menu')
  await expect(menu.getByText('党群人力部')).toBeVisible()
  await expect(menu.getByText('综合管理部')).toBeVisible()
  await expect(menu.getByText('设置')).toBeVisible()

  for (const header of ['姓名', '电话', '身份证号', '职位', '毕业院校', '地址', '操作']) {
    await expect(page.locator('.el-table__header').getByText(header, { exact: true })).toBeVisible()
  }
})
