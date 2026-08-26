import { NavLink, useNavigate } from 'react-router-dom'
import { useAuth } from '../../context/AuthContext'
import type { Opcion } from '../../types'

interface SectionItem extends Opcion {
  icon: string
}

interface Section {
  title: string
  items: SectionItem[]
}

function contains(nombre: string, keyword: string) {
  return nombre.toLowerCase().includes(keyword)
}

function buildSections(opciones: Opcion[]): Section[] {
  const sections: Section[] = [
    {
      title: 'Principal',
      items: opciones.filter((o) => contains(o.nombre, 'dashboard')).map((o) => ({ ...o, icon: 'bi-speedometer2' })),
    },
    {
      title: 'Productos',
      items: [
        ...opciones
          .filter(
            (o) =>
              contains(o.nombre, 'producto') &&
              !contains(o.nombre, 'categoria') &&
              !contains(o.nombre, 'marca') &&
              !contains(o.nombre, 'inventario') &&
              !contains(o.nombre, 'venta'),
          )
          .map((o) => ({ ...o, icon: 'bi-box-seam' })),
        ...opciones.filter((o) => contains(o.nombre, 'categoria')).map((o) => ({ ...o, icon: 'bi-grid-3x3-gap-fill' })),
        ...opciones.filter((o) => contains(o.nombre, 'marca')).map((o) => ({ ...o, icon: 'bi-star-fill' })),
      ],
    },
    {
      title: 'Operaciones',
      items: [
        ...opciones
          .filter((o) => contains(o.nombre, 'venta') && !contains(o.nombre, 'inventario'))
          .map((o) => ({ ...o, icon: 'bi-cart-fill' })),
        ...opciones.filter((o) => o.ruta === '/inventario').map((o) => ({ ...o, icon: 'bi-boxes' })),
      ],
    },
    {
      title: 'Administración',
      items: [
        ...opciones.filter((o) => contains(o.nombre, 'usuario')).map((o) => ({ ...o, icon: 'bi-people-fill' })),
        ...opciones
          .filter((o) => contains(o.nombre, 'perfil') || contains(o.nombre, 'rol') || contains(o.nombre, 'permiso'))
          .map((o) => ({ ...o, icon: 'bi-shield-lock-fill' })),
      ],
    },
    {
      title: 'Contactos',
      items: [
        ...opciones.filter((o) => contains(o.nombre, 'cliente')).map((o) => ({ ...o, icon: 'bi-person-badge-fill' })),
        ...opciones.filter((o) => contains(o.nombre, 'proveedor')).map((o) => ({ ...o, icon: 'bi-truck' })),
      ],
    },
    {
      title: 'Sistema',
      items: [
        ...opciones.filter((o) => contains(o.nombre, 'reporte')).map((o) => ({ ...o, icon: 'bi-bar-chart-fill' })),
        ...opciones.filter((o) => contains(o.nombre, 'personaliza')).map((o) => ({ ...o, icon: 'bi-palette-fill' })),
        ...opciones
          .filter((o) => contains(o.nombre, 'configuracion') || contains(o.nombre, 'config'))
          .map((o) => ({ ...o, icon: 'bi-gear-fill' })),
      ],
    },
  ]

  return sections.filter((section) => section.items.length > 0)
}

export default function Sidebar({ open, onClose }: { open: boolean; onClose: () => void }) {
  const { usuario, logout } = useAuth()
  const navigate = useNavigate()
  const sections = buildSections(usuario?.perfil?.opciones ?? [])

  async function handleLogout() {
    await logout()
    navigate('/login', { replace: true })
  }

  return (
    <>
      {open && <div className="fixed inset-0 z-[999] bg-black/50 md:hidden" onClick={onClose} />}
      <nav
        className={`fixed top-0 z-[1000] flex h-screen w-[280px] flex-col overflow-y-auto p-3 text-black transition-[left] duration-300 ease-in-out md:left-0 ${
          open ? 'left-0' : '-left-[280px]'
        }`}
        style={{ background: 'linear-gradient(135deg, #ffffff 0%, #b0b0b0 50%, #2c2c2c 100%)' }}
      >
        <div className="relative mb-3 text-center">
          <img src="/images/logoX.png" alt="Logo" className="mx-auto block h-auto max-w-[240px]" />
          <button
            className="absolute right-0 top-0 rounded border border-white/60 px-2 py-1 text-white md:hidden"
            onClick={onClose}
            aria-label="Cerrar menú"
          >
            <i className="bi bi-x-lg" />
          </button>
        </div>

        <div className="mb-auto flex-1 overflow-y-auto overflow-x-hidden pr-2">
          {sections.map((section) => (
            <div className="mb-4" key={section.title}>
              <h6 className="mb-1 mt-3 px-3 text-xs font-semibold uppercase tracking-wide text-black/60">
                <span className="inline-block border-b-2 border-black/10 pb-1">{section.title}</span>
              </h6>
              <ul className="list-none p-0">
                {section.items.map((item) => (
                  <li key={item.id}>
                    <NavLink
                      to={item.ruta}
                      onClick={onClose}
                      className={({ isActive }) =>
                        `mb-1 flex items-center gap-2 rounded-lg px-3 py-2 transition ${
                          isActive
                            ? 'translate-x-1 bg-black/20 font-semibold text-white shadow'
                            : 'text-black hover:translate-x-0.5 hover:bg-black/5 hover:text-[#666]'
                        }`
                      }
                    >
                      <i className={`bi ${item.icon}`} />
                      <span>{item.nombre}</span>
                    </NavLink>
                  </li>
                ))}
              </ul>
            </div>
          ))}
        </div>

        <hr className="border-black/10" />
        <ul className="list-none p-0">
          <li>
            <button
              onClick={handleLogout}
              className="flex w-full items-center gap-2 rounded-lg px-3 py-2 text-left font-medium text-brand-danger transition hover:bg-brand-danger/10 hover:text-brand-danger-hover"
            >
              <i className="bi bi-box-arrow-right" />
              Cerrar Sesión
            </button>
          </li>
        </ul>
      </nav>
    </>
  )
}
