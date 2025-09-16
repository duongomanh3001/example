/** @type {import('tailwindcss').Config} */
module.exports = {
  content: [
    './src/**/*.{js,ts,jsx,tsx,mdx}',
    './components/**/*.{js,ts,jsx,tsx,mdx}',
    './app/**/*.{js,ts,jsx,tsx,mdx}',
  ],
  theme: {
    extend: {
      fontFamily: {
        'sans': ['var(--font-open-sans)', 'Noto Sans Arabic', '-apple-system', 'BlinkMacSystemFont', 'Segoe UI', 'Roboto', 'Helvetica Neue', 'Arial', 'sans-serif', 'Apple Color Emoji', 'Segoe UI Emoji', 'Segoe UI Symbol'],
        'open-sans': ['var(--font-open-sans)', 'sans-serif'],
      },
      colors: {
        'primary': {
          DEFAULT: 'rgb(25, 72, 102)',
          50: 'rgb(240, 245, 248)',
          100: 'rgb(225, 235, 242)',
          200: 'rgb(195, 215, 230)',
          300: 'rgb(165, 195, 218)',
          400: 'rgb(105, 155, 194)',
          500: 'rgb(25, 72, 102)',
          600: 'rgb(20, 58, 82)',
          700: 'rgb(15, 44, 62)',
          800: 'rgb(10, 29, 41)',
          900: 'rgb(5, 15, 21)',
        },
        'cscore': {
          primary: 'rgb(25, 72, 102)',
          hover: 'rgb(20, 58, 82)',
          light: 'rgb(33, 89, 125)',
        }
      },
      textColor: {
        'primary': 'rgb(25, 72, 102)',
      }
    },
  },
  plugins: [],
}
