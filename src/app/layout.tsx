import type { Metadata } from "next";
import { Open_Sans, Noto_Sans } from "next/font/google";
import "./globals.css";
import ClientOnlyWrapper from "@/components/common/ClientOnlyWrapper";
import { AuthProvider } from "@/contexts/AuthContext";
import { NotificationProvider } from "@/contexts/NotificationContext";

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
  title: "CScore",
  description: "Hệ thống chấm điểm tự động CScore",
  icons: {
    icon: "/cscore-logo.ico",
  },
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
        suppressHydrationWarning
      >
        <AuthProvider>
          <NotificationProvider>
            <ClientOnlyWrapper />
            {children}
          </NotificationProvider>
        </AuthProvider>
      </body>
    </html>
  );
}
