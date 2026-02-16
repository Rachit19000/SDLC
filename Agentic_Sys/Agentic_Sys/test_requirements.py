from agents.requirements import requirements_agent

sample_text = """
Users should be able to upload documents.
The system should generate user stories.
Users must review and approve the output.
"""

artifact = requirements_agent(sample_text)

print(artifact)
print(artifact.model_dump_json(indent=2))
