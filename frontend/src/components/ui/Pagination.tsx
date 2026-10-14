interface PaginationProps {
  page: number
  pageSize: number
  totalPages: number
  totalElements: number
  onPageChange: (page: number) => void
}

/** Números visibles: primera, última y las cercanas a la actual, con "…" en los saltos. */
function visiblePages(page: number, totalPages: number): (number | 'gap')[] {
  const pages: (number | 'gap')[] = []
  for (let i = 0; i < totalPages; i++) {
    if (i === 0 || i === totalPages - 1 || Math.abs(i - page) <= 1) {
      pages.push(i)
    } else if (pages[pages.length - 1] !== 'gap') {
      pages.push('gap')
    }
  }
  return pages
}

export default function Pagination({ page, pageSize, totalPages, totalElements, onPageChange }: PaginationProps) {
  if (totalElements === 0) return null

  const from = page * pageSize + 1
  const to = Math.min((page + 1) * pageSize, totalElements)
  const button =
    'min-w-9 rounded-lg border px-3 py-1.5 text-sm transition disabled:cursor-not-allowed disabled:opacity-40'

  return (
    <div className="mt-4 flex flex-wrap items-center justify-between gap-3 text-sm text-slate-600">
      <span>
        Mostrando {from}–{to} de {totalElements}
      </span>
      {totalPages > 1 && (
        <nav className="flex flex-wrap items-center gap-1" aria-label="Paginación">
          <button
            type="button"
            onClick={() => onPageChange(page - 1)}
            disabled={page === 0}
            className={`${button} border-slate-300 hover:bg-slate-100`}
            aria-label="Página anterior"
          >
            <i className="bi bi-chevron-left" />
          </button>
          {visiblePages(page, totalPages).map((p, i) =>
            p === 'gap' ? (
              <span key={`gap-${i}`} className="px-1 text-slate-400">
                …
              </span>
            ) : (
              <button
                key={p}
                type="button"
                onClick={() => onPageChange(p)}
                aria-current={p === page ? 'page' : undefined}
                className={`${button} ${
                  p === page
                    ? 'border-brand-primary bg-brand-primary text-white'
                    : 'border-slate-300 hover:bg-slate-100'
                }`}
              >
                {p + 1}
              </button>
            ),
          )}
          <button
            type="button"
            onClick={() => onPageChange(page + 1)}
            disabled={page >= totalPages - 1}
            className={`${button} border-slate-300 hover:bg-slate-100`}
            aria-label="Página siguiente"
          >
            <i className="bi bi-chevron-right" />
          </button>
        </nav>
      )}
    </div>
  )
}
