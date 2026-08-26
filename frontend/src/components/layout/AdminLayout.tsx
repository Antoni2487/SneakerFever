import { useState } from 'react'
import { Outlet } from 'react-router-dom'
import Sidebar from './Sidebar'

export default function AdminLayout() {
  const [sidebarOpen, setSidebarOpen] = useState(false)

  return (
    <div className="flex">
      <button
        className="fixed left-[15px] top-[15px] z-[1001] flex h-[50px] w-[50px] items-center justify-center rounded-[10px] text-white shadow-lg transition hover:scale-105 md:hidden"
        style={{ background: 'linear-gradient(135deg, #1e1e1e 0%, #5a5a5a 50%, #f9f9f9 100%)' }}
        onClick={() => setSidebarOpen(true)}
        aria-label="Abrir menú"
      >
        <i className="bi bi-list text-xl" />
      </button>

      <Sidebar open={sidebarOpen} onClose={() => setSidebarOpen(false)} />

      <main className="w-full min-h-screen pl-[70px] transition-[margin-left] duration-300 ease-in-out md:ml-[280px] md:pl-0">
        <Outlet />
      </main>
    </div>
  )
}
