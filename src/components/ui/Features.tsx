"use client";

import Image from "next/image";
import { title } from "process";

export default function Features() {

  return (
    <section className="py-16 bg-gray-50">
      <div className="mx-auto max-w-7xl px-4">
        <div className="text-center mb-12">
          <div className="flex items-center justify-center gap-3 mb-4">
            <Image
              src="/icon/feature.png"
              alt="Tính năng hệ thống"
              width={40}
              height={40}
              className="w-10 h-10 object-contain"
            />
          </div>
          <p className="text-lg text-gray-600 max-w-2xl mx-auto">
            "not a bug, it's a feature"
          </p>
        </div>
        

        <div className="mt-16 bg-white rounded-xl p-8 shadow-sm">
          <div className="grid grid-cols-1 lg:grid-cols-2 gap-8 items-center">
            <div>
              <h3 className="text-2xl font-bold text-gray-900 mb-4">
                Bắt đầu với CSCORE
              </h3>
              <p className="text-gray-600 mb-6">
                Cùng nhau xây dựng kiến thức vững chắc trong lĩnh vực lập trình.
              </p>
              <div className="flex flex-col sm:flex-row gap-4">
                <a 
                  href="/login"
                  className="bg-primary text-white px-6 py-3 rounded-lg font-medium hover:bg-primary-600 transition-colors duration-200 text-center"
                >
                  Đăng nhập
                </a>
                <a 
                  href="#contact"
                  className="border border-gray-300 text-gray-700 px-6 py-3 rounded-lg font-medium hover:bg-gray-50 transition-colors duration-200 text-center"
                >
                  Tìm hiểu thêm
                </a>
              </div>
            </div>
            <div className="relative">
              <div className="aspect-video bg-gradient-to-br from-primary/10 to-primary/5 rounded-lg flex items-center justify-center">
                <Image
                  src="/cscore.png"
                  alt="CScore System"
                  width={300}
                  height={200}
                  className="object-contain"
                />
              </div>
            </div>
          </div>
        </div>
      </div>
    </section>
  );
}