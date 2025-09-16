// import Image from "next/image";
// export default function Hero() {
//   return (
//     <section className="relative overflow-hidden bg-gradient-to-br from-[#258aff] via-[#2f77ff] to-[#4fa3ff]">
//       {/* decorative dots */}
//       <div className="pointer-events-none absolute inset-0">
//         <svg className="absolute -left-10 top-20 opacity-40" width="120" height="120">
//           <circle cx="20" cy="20" r="4" fill="white"/>
//           <circle cx="60" cy="80" r="6" fill="white"/>
//           <circle cx="100" cy="40" r="3" fill="white"/>
//         </svg>
//       </div>

//       <div className="mx-auto max-w-7xl px-4 py-24 grid gap-8 lg:grid-cols-2 items-center">
//         <div>
//           <div className=" text-white rounded-xl px-5 py-4 inline-block">
//             <h1 className="text-2xl sm:text-2xl font-arial tracking-tight">
//               HỆ THỐNG CHẤM ĐIỂM TỰ ĐỘNG CSCORE
//             </h1>
//           </div>
//         </div>
//         <div className="relative hidden lg:block">
//           <div className="rounded-xl ring-1 ring-black/10 shadow-2xl overflow-hidden bg-white">
//               <Image src="/iuh.jpg" alt="preview" width={640} height={360} className="w-full h-auto"/>
//           </div>
//         </div>
//       </div>
//     </section>
//   );
// }


"use client";

import Image from "next/image";
import { Swiper, SwiperSlide } from "swiper/react";
import { Autoplay, Pagination } from "swiper/modules";

import "swiper/css";
import "swiper/css/pagination";

export default function Hero() {
  const images = ["/1.jpg", "/2.jpg", "/3.jpg"]; // danh sách ảnh

  return (
    <section className="relative overflow-hidden bg-gradient-to-br from-primary via-primary-600 to-primary-500">
      {/* decorative dots */}
      <div className="pointer-events-none absolute inset-0">
        <svg
          className="absolute -left-10 top-20 opacity-40"
          width="120"
          height="120"
        >
          <circle cx="20" cy="20" r="4" fill="white" />
          <circle cx="60" cy="80" r="6" fill="white" />
          <circle cx="100" cy="40" r="3" fill="white" />
        </svg>
      </div>

      <div className="mx-auto max-w-7xl px-4 py-24 grid gap-8 lg:grid-cols-2 items-center">
        {/* Left content */}
        <div>
          <div className="text-white rounded-xl px-5 py-4 inline-block">
            <h1 className="text-2xl sm:text-2xl font-arial tracking-tight">
              RAUMANIA SCORE SYSTEM
            </h1>
          </div>
        </div>

        {/* Right content (slideshow) */}
        <div className="relative hidden lg:block">
          <div className="rounded-xl shadow-2xl overflow-hidden relative">
            <Swiper
              modules={[Autoplay, Pagination]}
              autoplay={{ delay: 3000 }}
              loop={true}
              pagination={{ clickable: true }}
              className="w-full h-[360px] bg-transparent"
            >
              {images.map((img, idx) => (
                <SwiperSlide
                  key={idx}
                  className="flex items-center justify-center bg-transparent"
                >
                  <div className="relative w-full h-[360px]">
                    <Image
                      src={img}
                      alt={`slide-${idx}`}
                      fill
                      className="object-contain"
                      sizes="(max-width: 768px) 100vw, 640px"
                    />
                  </div>
                </SwiperSlide>
              ))}
            </Swiper>
          </div>
        </div>
      </div>
    </section>
  );
}


