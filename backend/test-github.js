// Test script to verify GitHub integration
const { createFileInGitHub } = require('./github-config');

async function testGitHub() {
  console.log('Testing GitHub integration...\n');
  
  try {
    const testEmail = 'rachitjainemail@gmail.com';
    const testPath = 'requirements/test/test_file.txt';
    const testContent = 'This is a test file to verify GitHub integration works!';
    const testMessage = 'Test: Verify GitHub API connection';
    
    console.log('Attempting to create file in GitHub...');
    console.log('Email:', testEmail);
    console.log('Path:', testPath);
    console.log('Message:', testMessage);
    console.log('');
    
    const result = await createFileInGitHub(testEmail, testPath, testContent, testMessage);
    
    console.log('✅ SUCCESS! File created in GitHub');
    console.log('File URL:', result.fileUrl);
    console.log('Commit URL:', result.commitUrl);
    console.log('\nCheck your GitHub repo to see the test file!');
    
  } catch (error) {
    console.error('❌ ERROR:', error.message);
    console.error('\nPossible issues:');
    console.error('1. GitHub token is invalid or expired');
    console.error('2. Token does not have "repo" permission');
    console.error('3. Repository does not exist or token has no access');
    console.error('4. Network connection issue');
    process.exit(1);
  }
}

testGitHub();
