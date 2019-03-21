# jdk-11-patches

Set of monkey patches for JDK 11

# Building

    mvn package
    
# Deploying to your internal Nexus repo

    mvn deploy -DaltDeploymentRepository=releases::default::http_url_for_your_local_nexus_content_repositories_releases
