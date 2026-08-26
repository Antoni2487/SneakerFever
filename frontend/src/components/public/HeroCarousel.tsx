import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'

interface HeroSlideData {
  key: string | number
  imagenUrl: string
  marcaId?: number
}

export default function HeroCarousel({ slides }: { slides: HeroSlideData[] }) {
  const [index, setIndex] = useState(0)

  useEffect(() => {
    if (slides.length <= 1) return
    const timer = setInterval(() => setIndex((i) => (i + 1) % slides.length), 5000)
    return () => clearInterval(timer)
  }, [slides.length])

  useEffect(() => {
    setIndex(0)
  }, [slides.length])

  if (slides.length === 0) return null

  return (
    <div className="relative aspect-[16/7] w-full overflow-hidden bg-black sm:aspect-[16/6]">
      {slides.map((slide, i) => {
        const img = (
          <img
            src={slide.imagenUrl}
            alt={`Slide ${i + 1}`}
            className={`absolute inset-0 h-full w-full object-cover transition-opacity duration-700 ${i === index ? 'opacity-100' : 'opacity-0'}`}
          />
        )
        return slide.marcaId ? (
          <Link key={slide.key} to={`/catalogo?marca=${slide.marcaId}`} className="contents">
            {img}
          </Link>
        ) : (
          <span key={slide.key} className="contents">
            {img}
          </span>
        )
      })}

      {slides.length > 1 && (
        <>
          <button
            onClick={() => setIndex((i) => (i - 1 + slides.length) % slides.length)}
            aria-label="Anterior"
            className="absolute left-3 top-1/2 z-10 flex h-9 w-9 -translate-y-1/2 items-center justify-center rounded-full bg-white/80 text-black transition hover:bg-white"
          >
            <i className="bi bi-chevron-left" />
          </button>
          <button
            onClick={() => setIndex((i) => (i + 1) % slides.length)}
            aria-label="Siguiente"
            className="absolute right-3 top-1/2 z-10 flex h-9 w-9 -translate-y-1/2 items-center justify-center rounded-full bg-white/80 text-black transition hover:bg-white"
          >
            <i className="bi bi-chevron-right" />
          </button>
          <div className="absolute bottom-3 left-1/2 z-10 flex -translate-x-1/2 gap-1.5">
            {slides.map((slide, i) => (
              <button
                key={slide.key}
                onClick={() => setIndex(i)}
                aria-label={`Ir al slide ${i + 1}`}
                className={`h-2 rounded-full transition-all ${i === index ? 'w-6 bg-white' : 'w-2 bg-white/50'}`}
              />
            ))}
          </div>
        </>
      )}
    </div>
  )
}
