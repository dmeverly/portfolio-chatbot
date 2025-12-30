.PHONY: demo run-private run-private-prod package-private test-private secscan secscan-update

demo:
	@echo "Running Demo"
	SPRING_PROFILES_ACTIVE=demo mvn spring-boot:run

run-private:
	@echo "Running Private"
	@set -a; \
	. ./.env; \
	set +a; \
	SPRING_PROFILES_ACTIVE=private mvn -Pprivate spring-boot:run

run-production:
	@echo "Running Production"
	@set -a; \
	. ./.env; \
	set +a; \
	SPRING_PROFILES_ACTIVE=private,prod mvn -Pprivate spring-boot:run

package-production:
	@echo "Packaging Production"
	@set -a; \
	. ./.env; \
	set +a; \
	SPRING_PROFILES_ACTIVE=private,prod mvn -Pprivate clean package

test-private:
	@echo "Running private tests"
	@if [ ! -f .env ]; then echo "ERROR: .env missing"; exit 1; fi
	@set -a; \
	. ./.env; \
	set +a; \
	mvn -Pprivate -Dtest=dev.everly.portfolio.api.ChatControllerPolicyTest test

secscan:
	@echo "Running OWASP Dependency-Check using cached DB"
	mvn -DskipTests -Ddependency-check.autoUpdate=false verify

secscan-update:
	@echo "Running OWASP Dependency-Check"
	@echo "Requires NVD_API_KEY in environment"
	mvn -DskipTests -Ddependency-check.autoUpdate=true verify