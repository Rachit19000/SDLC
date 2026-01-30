// Test GitHub token and repository access
const { Octokit } = require('@octokit/rest');

const GITHUB_TOKEN = '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';
const octokit = new Octokit({ auth: GITHUB_TOKEN });

async function testToken() {
  console.log('Testing GitHub token...\n');
  
  try {
    // Test 1: Get authenticated user
    console.log('1. Testing token authentication...');
    const { data: user } = await octokit.users.getAuthenticated();
    console.log('✅ Token is valid!');
    console.log('   Authenticated as:', user.login);
    console.log('');
    
    // Test 2: Check repository access
    console.log('2. Testing repository access...');
    try {
      const { data: repo } = await octokit.repos.get({
        owner: 'Rachit19000',
        repo: 'files_storage'
      });
      console.log('✅ Repository found!');
      console.log('   Name:', repo.full_name);
      console.log('   Private:', repo.private);
      console.log('   Default branch:', repo.default_branch);
      console.log('');
    } catch (error) {
      console.error('❌ Cannot access repository:', error.message);
      console.error('   Status:', error.status);
      if (error.status === 404) {
        console.error('   Repository might not exist or token has no access');
      }
      return;
    }
    
    // Test 3: Check if we can read contents
    console.log('3. Testing read access...');
    try {
      const { data } = await octokit.repos.getContent({
        owner: 'Rachit19000',
        repo: 'files_storage',
        path: 'README.md',
        ref: 'main'
      });
      console.log('✅ Can read repository contents!');
      console.log('');
    } catch (error) {
      console.error('❌ Cannot read repository:', error.message);
      return;
    }
    
    // Test 4: Try to create a file
    console.log('4. Testing write access (creating test file)...');
    try {
      const testContent = Buffer.from('GitHub integration test').toString('base64');
      const { data } = await octokit.repos.createOrUpdateFileContents({
        owner: 'Rachit19000',
        repo: 'files_storage',
        path: 'test_write_access.txt',
        message: 'Test: Verify write access',
        content: testContent,
        branch: 'main'
      });
      console.log('✅ Can write to repository!');
      console.log('   File URL:', data.content.html_url);
      console.log('');
      console.log('🎉 All tests passed! GitHub integration should work.');
    } catch (error) {
      console.error('❌ Cannot write to repository:', error.message);
      console.error('   Status:', error.status);
      console.error('   Response:', error.response?.data);
      console.error('\n   This means the token does not have write permissions.');
      console.error('   Make sure the token has "repo" scope enabled.');
    }
    
  } catch (error) {
    console.error('❌ Token test failed:', error.message);
    console.error('   Status:', error.status);
    if (error.status === 401) {
      console.error('\n   Token is invalid or expired. Generate a new token.');
    }
  }
}

testToken();
