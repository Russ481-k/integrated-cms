/** @type {import('next').NextConfig} */
const nextConfig = {
  env: {
    NEXT_PUBLIC_SERVICE_TYPE: process.env.NEXT_PUBLIC_SERVICE_TYPE || 'integrated-cms',
    NEXT_PUBLIC_SERVICE_NAME: process.env.NEXT_PUBLIC_SERVICE_NAME || '통합 CMS 관리',
    NEXT_PUBLIC_API_BASE_URL: process.env.NEXT_PUBLIC_API_BASE_URL || 'http://localhost:8080/api/v2/integrated-cms',
  },
  async rewrites() {
    return [
      {
        source: '/api/:path*',
        destination: `${process.env.NEXT_PUBLIC_API_BASE_URL || 'http://localhost:8080/api/v2/integrated-cms'}/:path*`,
      },
    ];
  },
};

module.exports = nextConfig;