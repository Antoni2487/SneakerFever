import { useState } from 'react'

interface ImageThumbnailProps {
  src: string | null
  alt?: string
  className?: string
}

export default function ImageThumbnail({ src, alt = 'Imagen', className }: ImageThumbnailProps) {
  const [broken, setBroken] = useState(false)
  const boxClass = className ?? 'h-[50px] w-[80px]'

  if (!src || broken) {
    return (
      <div className={`mx-auto flex items-center justify-center rounded bg-slate-100 text-[10px] text-slate-400 ${boxClass}`}>
        Sin imagen
      </div>
    )
  }

  return (
    <img
      src={src}
      alt={alt}
      onError={() => setBroken(true)}
      className={`mx-auto block object-contain ${boxClass}`}
    />
  )
}
