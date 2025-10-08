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
  const images = ["/icon/code-1.jpg", "/icon/code-2.jpg", "/icon/code-3.jpg"]; // danh sách ảnh

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
        <div className="space-y-6">
          <div className="text-white rounded-xl px-5 py-4 inline-block">
            <h1 className="text-3xl sm:text-4xl font-bold tracking-tight">
              CSCORE
            </h1>
            <h2 className="text-lg sm:text-xl mt-2 opacity-90">
              Hệ thống học tập trực tuyến
            </h2>
          </div>
          
          <div className="text-white space-y-4 max-w-lg">
            <p className="text-sm sm:text-base leading-relaxed">
              Chào mừng các bạn đến với kênh học tập trực tuyến của CSCORE, hệ thống cung cấp cho sinh viên những khóa học trực tuyến song song với các lớp học trên lớp mà sinh viên đang học tại trường.
            </p>
            <p className="text-sm sm:text-base leading-relaxed">
              Trong thời gian tham gia khóa học, sinh viên vui lòng làm bài tập theo yêu cầu đầy đủ. Chúc các bạn trang bị thêm được nhiều kiến thức trên kênh học trực tuyến này
            </p>
            
            <div className="mt-6">
              <div className="flex flex-wrap gap-2">
                <span className="bg-white/20 backdrop-blur-sm px-3 py-1 rounded-full text-sm font-medium flex items-center gap-2">
                  <Image
                    src="/icon/java-language.png"
                    alt="Java"
                    width={20}
                    height={20}
                    className="w-5 h-5 object-contain"
                  />
                  Java
                </span>
                <span className="bg-white/20 backdrop-blur-sm px-3 py-1 rounded-full text-sm font-medium flex items-center gap-2">
                  <Image
                    src="/icon/c-language.png"
                    alt="C/C++"
                    width={20}
                    height={20}
                    className="w-5 h-5 object-contain"
                  />
                  C/C++
                </span>
                <span className="bg-white/20 backdrop-blur-sm px-3 py-1 rounded-full text-sm font-medium flex items-center gap-2">
                  <Image
                    src="/icon/python.png"
                    alt="Python"
                    width={20}
                    height={20}
                    className="w-5 h-5 object-contain"
                  />
                  Python
                </span>
              </div>
            </div>
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


