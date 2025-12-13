module.exports = {
  // Development configuration for API proxy
  dev: {
    '/api': {
      target: 'http://localhost:8080',
      changeOrigin: true,
      secure: false,
      logLevel: 'debug',
    }
  }
};