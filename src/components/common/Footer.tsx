export default function Footer() {
  return (
    <footer className="bg-primary text-white mt-20">
      <div className="mx-auto max-w-7xl px-4 py-12 grid gap-8 md:grid-cols-2">
        <div>
          <div className="flex items-center gap-2 semibold text-lg">
            <img src="/cscore.png" alt="CSCORE" className="h-6 w-6" />
            <span className="text-white">CSCORE - Raumania Score System</span>
          </div>
        </div>
        <div className="grid sm:grid-cols-2 gap-6">
          <div>
            <h4 className="text-white font-semibold mb-3">Liên kết</h4>
            <ul className="space-y-2 text-sm">
              <li><a href="#" className="text-white hover:text-primary-200 hover:underline">Website Nhà Trường</a></li>
              <li><a href="#" className="text-white hover:text-primary-200 hover:underline">Website Trung tâm CNHT</a></li>
              <li><a href="#" className="text-white hover:text-primary-200 hover:underline">Cổng Thông Tin Sinh Viên</a></li>
              <li><a href="#courses" className="text-white hover:text-primary-200 hover:underline">Các Khóa Học</a></li>
            </ul>
          </div>
          <div>
            <h4 className="text-white font-semibold mb-3">Liên hệ</h4>
            <div className="text-sm space-y-2 text-white">
              <p><span className="font-semibold">Trung tâm Quản trị</span>COUNTER-SCORE 36</p>
              <p>Phone: 098 989 999 - ext 420</p>
              <p>E-mail: cs36@raumania.edu.vn</p>
            </div>
          </div>
        </div>
      </div>
      <div className="border-t border-primary-400">
        <div className="mx-auto max-w-7xl px-4 py-4 text-xs text-center text-primary-200">
          Copyright © 2025 - CSCORE
        </div>
      </div>
    </footer>
  );
}
