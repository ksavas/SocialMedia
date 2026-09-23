const fs = require('fs');
const path = require('path');

const envFilePath = path.join(__dirname, '.env');
if (fs.existsSync(envFilePath)) {
  const lines = fs.readFileSync(envFilePath, 'utf8').split('\n');
  for (const line of lines) {
    const trimmed = line.trim();
    if (!trimmed || trimmed.startsWith('#')) continue;
    const eqIndex = trimmed.indexOf('=');
    if (eqIndex === -1) continue;
    const key = trimmed.substring(0, eqIndex).trim();
    const value = trimmed.substring(eqIndex + 1).trim().replace(/^["']|["']$/g, '');
    if (!process.env[key]) process.env[key] = value;
  }
}

const googleClientId = process.env.GOOGLE_CLIENT_ID || 'not-configured';

if (googleClientId === 'not-configured') {
  console.warn('Warning: GOOGLE_CLIENT_ID is not set, Google Sign-In will be unavailable.');
  console.warn('Set it in the terminal or create a frontend/.env file:');
  console.warn('  GOOGLE_CLIENT_ID=your-client-id.apps.googleusercontent.com');
}

const api = {
  user: process.env.USER_API_URL || 'http://localhost:8081',
  post: process.env.POST_API_URL || 'http://localhost:8082',
  feed: process.env.FEED_API_URL || 'http://localhost:8084',
  notification: process.env.NOTIFICATION_API_URL || 'http://localhost:8085',
};

const content = `export const environment = {
  production: false,
  googleClientId: '${googleClientId}',
  api: {
    user: '${api.user}',
    post: '${api.post}',
    feed: '${api.feed}',
    notification: '${api.notification}',
  },
};\n`;

const outputDir = path.join(__dirname, 'src', 'environments');
fs.mkdirSync(outputDir, { recursive: true });
fs.writeFileSync(path.join(outputDir, 'environment.ts'), content, 'utf8');
console.log('environment.ts generated.');
