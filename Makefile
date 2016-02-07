intall: 
	@./gradlew install
update:
	@./gradlew install --refresh-dependencies
test:
	@./gradlew test --tests Tester
adhoc:
	@./gradlew test --tests Adhoc
ros:
	@./gradlew test --tests RosRun
idea: 
	@./gradlew idea
doc:
	@./gradlew javadoc
clean: 
	@./gradlew clean 
