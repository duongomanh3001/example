import type { Metadata } from "next";
import { Open_Sans, Noto_Sans } from "next/font/google";
import "./globals.css";
import ClientOnlyWrapper from "@/components/common/ClientOnlyWrapper";
import { AuthProvider } from "@/contexts/AuthContext";

const openSans = Open_Sans({
  variable: "--font-open-sans",
  subsets: ["latin"],
  display: "swap",
});

const notoSansArabic = Noto_Sans({
  variable: "--font-noto-sans-arabic",
  subsets: ["latin"],
  display: "swap",
});

export const metadata: Metadata = {
  title: "CScore - Hệ thống chấm điểm tự động",
  description: "Hệ thống chấm điểm tự động CScore - Đổi mới tư duy, làm giàu thêm tri thức",
};

export default function RootLayout({
  children,
}: Readonly<{
  children: React.ReactNode;
}>) {
  return (
    <html lang="en" suppressHydrationWarning>
      <body
        className={`${openSans.variable} ${notoSansArabic.variable} antialiased`}
      >
        <AuthProvider>
          <ClientOnlyWrapper />
          {children}
        </AuthProvider>
      </body>
    </html>
  );
}
