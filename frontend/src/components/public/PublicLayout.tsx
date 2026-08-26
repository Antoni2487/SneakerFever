import { Outlet } from 'react-router-dom'
import Navbar from './Navbar'
import Footer from './Footer'

export default function PublicLayout() {
  return (
    <div className="min-h-screen bg-[#f8f8f8] font-poppins text-[#222]">
      <Navbar />
      <Outlet />
      <Footer />

      <a
        href="https://wa.me/51993903939?text=Hola%20Sneackers%20Fever!%20Tengo%20una%20duda."
        target="_blank"
        rel="noreferrer"
        aria-label="Atención al cliente por WhatsApp"
        className="fixed bottom-5 right-5 z-40 flex h-14 w-14 items-center justify-center rounded-full bg-brand-success text-2xl text-white shadow-lg transition hover:scale-105"
      >
        <i className="bi bi-whatsapp" />
      </a>
    </div>
  )
}
