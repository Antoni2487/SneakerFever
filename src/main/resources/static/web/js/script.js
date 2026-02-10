document.addEventListener("DOMContentLoaded", () => {
  // Swiper slider principal (Hero)
  new Swiper('.mySwiper1', {
    loop: true,
    speed: 800,
    autoplay: {
      delay: 4000,
      disableOnInteraction: false
    },
    navigation: {
      nextEl: '.swiper-button-next',
      prevEl: '.swiper-button-prev'
    },
    pagination: {
      el: '.swiper-pagination',
      clickable: true
    },
  });

  // Swiper para Zapatillas del Mes - SIN PAGINACIÓN
  new Swiper('.mySwiper2', {
    // Slides visibles
    slidesPerView: 'auto',
    spaceBetween: 30,
    centeredSlides: true,
    
    // AUTO-PLAY activado
    autoplay: {
      delay: 3500,
      disableOnInteraction: false,
      pauseOnMouseEnter: true
    },
    
    // Loop infinito
    loop: true,
    
    // Velocidad de transición
    speed: 800,
    
    // Navegación con teclado (opcional)
    keyboard: {
      enabled: true,
      onlyInViewport: true
    },
    
    // Responsive breakpoints
    breakpoints: {
      320: {
        slidesPerView: 1,
        spaceBetween: 20,
        centeredSlides: true
      },
      768: {
        slidesPerView: 'auto',
        spaceBetween: 30,
        centeredSlides: true
      }
    }
  });

  // Rotación de imágenes en las cartas de Novedades
  document.querySelectorAll('.carta').forEach(carta => {
    const imgs = carta.querySelectorAll('img');
    let idx = 0;

    const next = () => {
      imgs.forEach(i => i.classList.remove('visible'));
      imgs[idx].classList.add('visible');
      idx = (idx + 1) % imgs.length;
    };

    if (imgs.length) {
      next();
      setInterval(next, 3000);
    }
  });

  // Navbar hide/show al hacer scroll
  let lastScrollTop = 0;
  const navbar = document.querySelector('.navbar');

  if (navbar) {
    window.addEventListener('scroll', function() {
      let scrollTop = window.pageYOffset || document.documentElement.scrollTop;

      if (scrollTop > lastScrollTop && scrollTop > 100) {
        navbar.classList.add('navbar-hidden');
      } else {
        navbar.classList.remove('navbar-hidden');
      }

      lastScrollTop = scrollTop <= 0 ? 0 : scrollTop;
    });
  }

  // Script para las pestañas
  const tabButtons = document.querySelectorAll('.tab-button');
  const tabContents = document.querySelectorAll('.tab-content');

  tabButtons.forEach(button => {
    button.addEventListener('click', function() {
      const targetTab = this.getAttribute('data-tab');

      tabButtons.forEach(btn => btn.classList.remove('active'));
      tabContents.forEach(content => content.classList.remove('active'));

      this.classList.add('active');
      document.getElementById(targetTab).classList.add('active');
    });
  });
});

// Función para cambiar imagen en cards de producto
function cambiarImagen(thumbnail) {
  const card = thumbnail.closest('.producto-card');
  const imagenPrincipal = card.querySelector('.imagen-principal img');
  if (imagenPrincipal) {
    imagenPrincipal.src = thumbnail.src;
  }
}