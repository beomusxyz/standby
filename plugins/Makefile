SRC_DIR   := src
DEV_DIR   := dev
BUILD_DIR := build

SRC_DIRS := $(notdir $(patsubst %/,%,$(wildcard $(SRC_DIR)/*/)))
DEV_DIRS := $(notdir $(patsubst %/,%,$(wildcard $(DEV_DIR)/*/)))

ZIP_TARGETS     := $(addprefix zip-,$(SRC_DIRS))
DEV_ZIP_TARGETS := $(addprefix devzip-,$(DEV_DIRS))

# fuck u windows
ifeq ($(OS),Windows_NT)
    MKDIR_BUILD = if not exist "$(BUILD_DIR)" mkdir "$(BUILD_DIR)"
    MKDIR_DEV   = if not exist "$(BUILD_DIR)\$(DEV_DIR)" mkdir "$(BUILD_DIR)\$(DEV_DIR)"
    ZIP_CMD     = cd "$(SRC_DIR)/$*" && tar -a -c -f "../../$(BUILD_DIR)/$*.zip" *
    DEV_ZIP_CMD = cd "$(DEV_DIR)/$*" && tar -a -c -f "../../$(BUILD_DIR)/$(DEV_DIR)/$*.zip" *
    CLEAN_CMD   = if exist "$(BUILD_DIR)" rmdir /s /q "$(BUILD_DIR)"
else
    MKDIR_BUILD = mkdir -p "$(BUILD_DIR)"
    MKDIR_DEV   = mkdir -p "$(BUILD_DIR)/$(DEV_DIR)"
    ZIP_CMD     = cd "$(SRC_DIR)/$*" && zip -r "../../$(BUILD_DIR)/$*.zip" *
    DEV_ZIP_CMD = cd "$(DEV_DIR)/$*" && zip -r "../../$(BUILD_DIR)/$(DEV_DIR)/$*.zip" *
    CLEAN_CMD   = rm -rf "$(BUILD_DIR)"
endif

.PHONY: all dev clean debug $(ZIP_TARGETS) $(DEV_ZIP_TARGETS)

all: $(ZIP_TARGETS)

# Dev plugins are test fixtures. They build into build/dev/ and are never shipped.
dev: $(DEV_ZIP_TARGETS)

$(ZIP_TARGETS): zip-%:
	@echo Zipping $*...
	@$(MKDIR_BUILD)
	@$(ZIP_CMD)

$(DEV_ZIP_TARGETS): devzip-%:
	@echo Zipping dev plugin $*...
	@$(MKDIR_DEV)
	@$(DEV_ZIP_CMD)

clean:
	@echo Cleaning up...
	@$(CLEAN_CMD)

debug:
	@echo OS=$(OS)
	@echo SRC_DIRS=$(SRC_DIRS)
	@echo DEV_DIRS=$(DEV_DIRS)
	@echo ZIP_TARGETS=$(ZIP_TARGETS)
	@echo DEV_ZIP_TARGETS=$(DEV_ZIP_TARGETS)
