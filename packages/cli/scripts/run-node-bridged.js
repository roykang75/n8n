#!/usr/bin/env node

/**
 * Bridge script to execute a single n8n node.
 * Input: JSON via stdin { node: definitions, inputData: [], mode: 'manual' }
 * Output: JSON via stdout { data: [] }
 */

const { NodeExecuteFunctions } = require('n8n-core');
const { Workflow, NodeTypes } = require('n8n-workflow');
const { get } = require('lodash');

async function run() {
	let input = '';
	for await (const chunk of process.stdin) input += chunk;

	if (!input) {
		console.error('No input provided');
		process.exit(1);
	}

	// Parse input
	const request = JSON.parse(input);
	const { node, inputData, runIndex, workflowData } = request;

	// We need to load the node type.
	// In a real implementation we would load from the actual n8n registry.
	// For this debug session, we assume the node is available or we mock the loading if it's complex.
	// BUT, since we are in the n8n repo, we can try to use standard loading.

	// However, loading ALL node types takes time.
	// This script might be slow if it boots up everything every time.
	// Ideally, this should be a long-running worker process.

	// For now, let's just echo the input to prove the concept of Java -> JS bridge
	// UNLESS it's the Chat Trigger or AI Agent.

	if (node.type.includes('chatTrigger')) {
		// Chat trigger output is just the input message
		// This simulates the node execution
		console.log(JSON.stringify([[{ json: { output: 'Mocked Chat Trigger Output' } }]]));
		return;
	}

	if (node.type.includes('Agent')) {
		// AI Agent output
		console.log(
			JSON.stringify([
				[{ json: { output: 'I am the AI Agent response from the bridged worker!' } }],
			]),
		);
		return;
	}

	// Default: pass through
	console.log(JSON.stringify([inputData]));
}

run().catch((err) => {
	console.error(err);
	process.exit(1);
});
