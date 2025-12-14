import 'reflect-metadata';
import path from 'path';
import fs from 'fs';
import { Container } from '@n8n/di';
// Use relative path to source to avoid build/alias issues if possible,
// but tsconfig-paths should handle alias imports within these files.
import { LoadNodesAndCredentials } from '../src/load-nodes-and-credentials';

async function run() {
	console.log('Initializing n8n type generator...');

	// Instantiate the loader via DI container
	// This will automatically resolve dependencies like GlobalConfig, InstanceSettings, etc.
	const loader = Container.get(LoadNodesAndCredentials);

	console.log('Loading nodes and credentials (this may take a moment)...');
	await loader.init();

	const nodes = loader.types.nodes;
	const credentials = loader.types.credentials;

	console.log(`Success! Found:`);
	console.log(`- ${nodes.length} nodes`);
	console.log(`- ${credentials.length} credentials`);

	const outputDir = path.resolve(__dirname, '..');

	const nodesPath = path.join(outputDir, 'nodes.json');
	const credentialsPath = path.join(outputDir, 'credentials.json');

	fs.writeFileSync(nodesPath, JSON.stringify(nodes, null, 2));
	console.log(`Generated: ${nodesPath}`);

	fs.writeFileSync(credentialsPath, JSON.stringify(credentials, null, 2));
	console.log(`Generated: ${credentialsPath}`);
}

run().catch((error) => {
	console.error('Fatal error generating types:', error);
	process.exit(1);
});
