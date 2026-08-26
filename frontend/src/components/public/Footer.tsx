import { Link } from 'react-router-dom'

export default function Footer() {
  return (
    <footer className="mt-16 bg-black pt-10 text-white">
      <div className="mx-auto grid max-w-7xl grid-cols-1 gap-8 px-4 pb-10 sm:grid-cols-2 lg:grid-cols-5 lg:px-8">
        <div>
          <h4 className="mb-3 text-sm font-bold tracking-wide">TIENDAS</h4>
          <p className="mb-2 text-sm text-slate-300">
            <strong className="text-white">FLAGSHIP ACACIAS</strong>
            <br />
            <em>Miraflores - Lima</em>
          </p>
          <p className="mb-2 text-sm text-slate-300">
            <strong className="text-white">TIENDA JOCKEY PLAZA</strong>
            <br />
            <em>Surco - Lima</em>
          </p>
          <p className="text-sm text-slate-300">
            <strong className="text-white">TIENDA SAN BORJA</strong>
            <br />
            <em>San Borja - Lima</em>
          </p>
        </div>

        <div>
          <h4 className="mb-3 text-sm font-bold tracking-wide">CUENTA</h4>
          <ul className="space-y-2 text-sm text-slate-300">
            <li>Mi Cuenta</li>
            <li>Registrarme</li>
            <li>Soporte</li>
            <li>Estado de pedido</li>
          </ul>
        </div>

        <div>
          <h4 className="mb-3 text-sm font-bold tracking-wide">INFORMACIÓN</h4>
          <ul className="space-y-2 text-sm text-slate-300">
            <li>Nosotros</li>
            <li>Políticas</li>
            <li>Términos y condiciones</li>
            <li>
              <Link to="/contacto" className="hover:text-white">
                Contáctanos
              </Link>
            </li>
          </ul>
        </div>

        <div>
          <h4 className="mb-3 text-sm font-bold tracking-wide">REDES SOCIALES</h4>
          <ul className="space-y-2 text-sm text-slate-300">
            <li>Instagram</li>
            <li>Facebook</li>
            <li>YouTube</li>
          </ul>
        </div>

        <div>
          <h4 className="mb-3 text-sm font-bold tracking-wide">ÚNETE A NUESTRO EQUIPO</h4>
          <p className="mb-3 text-sm text-slate-300">Trabaja con nosotros</p>
          <p className="text-sm text-slate-300">
            <strong className="text-white">SNEACKERS FEVER:</strong>
            <br />
            RUC: 20602981658
            <br />
            Razón Social: SNEACKERS FEVER S.A.C.
          </p>
        </div>
      </div>
      <div className="border-t border-white/10 py-4 text-center text-xs text-slate-400">© {new Date().getFullYear()} Sneakers Fever</div>
    </footer>
  )
}
