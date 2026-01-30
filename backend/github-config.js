// GitHub Configuration
// Maps user emails to their GitHub repositories

const { Octokit } = require('@octokit/rest');

// Load environment variables
try {
  require('dotenv').config();
} catch (e) {
  // dotenv not installed, continue without it
}

// GitHub Personal Access Token (from environment or hardcoded for specific user)
// Get this from: GitHub → Settings → Developer settings → Personal access tokens
const GITHUB_TOKEN = process.env.GITHUB_TOKEN || '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';

// Initialize Octokit
const octokit = new Octokit({
  auth: GITHUB_TOKEN
});

// Map user emails to their GitHub repositories
// Format: { email: { owner: 'username', repo: 'repo-name' } }
const userRepos = {
  'rachitjainemail@gmail.com': {
    owner: 'Rachit19000',
    repo: 'files_storage',
    branch: 'main'
  },
  // Add more users here as needed
  // 'other@example.com': {
  //   owner: 'otherusername',
  //   repo: 'other_repo',
  //   branch: 'main'
  // }
};

/**
 * Get GitHub repo info for a user email
 */
function getUserRepo(email) {
  return userRepos[email] || null;
}

/**
 * Create or update a file in GitHub
 */
async function createFileInGitHub(email, filePath, content, commitMessage) {
  const repoInfo = getUserRepo(email);
  
  if (!repoInfo) {
    throw new Error(`No GitHub repository configured for user: ${email}`);
  }

  // Encode content to base64 (GitHub API requirement)
  const encodedContent = Buffer.from(content).toString('base64');

  try {
    // Check if file already exists
    let sha = null;
    try {
      const { data } = await octokit.repos.getContent({
        owner: repoInfo.owner,
        repo: repoInfo.repo,
        path: filePath,
        ref: repoInfo.branch
      });
      sha = data.sha; // File exists, we need SHA for update
    } catch (error) {
      // File doesn't exist, that's okay - we'll create it
      if (error.status !== 404) {
        throw error;
      }
    }

    // Create or update file
    const { data } = await octokit.repos.createOrUpdateFileContents({
      owner: repoInfo.owner,
      repo: repoInfo.repo,
      path: filePath,
      message: commitMessage,
      content: encodedContent,
      branch: repoInfo.branch,
      ...(sha && { sha }) // Include SHA if updating existing file
    });

    return {
      success: true,
      fileUrl: data.content.html_url,
      commitUrl: data.commit.html_url,
      sha: data.content.sha
    };
  } catch (error) {
    console.error('GitHub API Error Details:', {
      status: error.status,
      message: error.message,
      response: error.response?.data
    });
    throw new Error(`Failed to create file in GitHub: ${error.message} (Status: ${error.status})`);
  }
}

module.exports = {
  octokit,
  getUserRepo,
  createFileInGitHub
};
