// Type declarations for CSS imports
declare module '*.css' {
  const content: any;
  export default content;
}

declare module '*.scss' {
  const content: any;
  export default content;
}

declare module '*.sass' {
  const content: any;
  export default content;
}

// Swiper CSS module declarations for side-effect imports
declare module 'swiper/css';
declare module 'swiper/css/navigation';
declare module 'swiper/css/pagination';
declare module 'swiper/css/scrollbar';
declare module 'swiper/css/autoplay';
declare module 'swiper/css/effect-fade';
declare module 'swiper/css/effect-cube';
declare module 'swiper/css/effect-flip';
declare module 'swiper/css/effect-coverflow';
declare module 'swiper/css/thumbs';
declare module 'swiper/css/free-mode';