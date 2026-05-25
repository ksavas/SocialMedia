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

const googleClientId = process.env.GOOGLE_CLIENT_ID;

if (!googleClientId) {
  console.error('Error: GOOGLE_CLIENT_ID environment variable is not set.');
  console.error('Set it in the terminal or create a frontend/.env file:');
  console.error('  GOOGLE_CLIENT_ID=your-client-id.apps.googleusercontent.com');
  process.exit(1);
}

const content = `export const environment = {
  production: false,
  googleClientId: '${googleClientId}',
};\n`;

const outputPath = path.join(__dirname, 'src', 'environments', 'environment.ts');
fs.writeFileSync(outputPath, content, 'utf8');
console.log('environment.ts generated.');
