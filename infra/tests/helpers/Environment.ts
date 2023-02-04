import * as dotenv from 'dotenv';

class Environment {
   envConfig;
   constructor() {
      this.envConfig = {
         ...this.readEnvFile('.env'),
         ...this.readEnvFile('.env.local'),
      };
   }

   readEnvFile(path) {
      const result = dotenv.config({ path });

      return result.error ? {} : result.parsed;
   }

   get(key, defaultValue = undefined) {
      const value = this.envConfig[key] || process.env[key];
      if (!value && defaultValue === undefined) {
         console.error(`\n${key} is required environment variable`);

         console.error(`\nYou should define ${key} environment variable
You can write it in .env.local file:
${key}=value\n`);

         process.exit(1);
      }

      return value || defaultValue;
   }
}

export const env = new Environment();
