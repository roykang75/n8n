const axios = require('axios');

async function createTestUser() {
  try {
    const response = await axios.post('http://localhost:8080/api/auth/register', {
      email: 'test@example.com',
      firstName: 'Test',
      lastName: 'User',
      password: 'password123',
      role: 'USER'
    });

    console.log('✅ Test user created:', response.data);

    // Now try to login
    const loginResponse = await axios.post('http://localhost:8080/api/auth/login', {
      email: 'test@example.com',
      password: 'password123'
    });

    console.log('✅ Login successful!');
    console.log('Token:', loginResponse.data.token);

    // Test API with token
    const workflowsResponse = await axios.get('http://localhost:8080/api/workflows', {
      headers: {
        'Authorization': `Bearer ${loginResponse.data.token}`
      }
    });

    console.log('✅ Workflows API works:', workflowsResponse.data);
  } catch (error) {
    console.error('❌ Error:', error.response?.data || error.message);
  }
}

createTestUser();