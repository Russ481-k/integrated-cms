/** @type {import('next').NextConfig} */
const nextConfig = {
  env: {
    NEXT_PUBLIC_SERVICE_TYPE: process.env.NEXT_PUBLIC_SERVICE_TYPE || 'douzone',
    NEXT_PUBLIC_SERVICE_NAME: process.env.NEXT_PUBLIC_SERVICE_NAME || '더존 CMS',
    NEXT_PUBLIC_API_BASE_URL: process.env.NEXT_PUBLIC_API_BASE_URL || 'http://localhost:8080/api/v2/cms/douzone',
  },
  async rewrites() {
    return [
      {
        source: '/api/:path*',
        destination: `${process.env.NEXT_PUBLIC_API_BASE_URL || 'http://localhost:8080/api/v2/cms/douzone'}/:path*`,
      },
    ];
  },
};

module.exports = nextConfig;