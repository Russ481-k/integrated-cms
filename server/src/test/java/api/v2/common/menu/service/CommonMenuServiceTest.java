package api.v2.common.menu.service;

import api.v2.common.menu.domain.Menu;
import api.v2.common.menu.domain.MenuType;
import api.v2.common.menu.repository.MenuRepository;
import api.v2.common.menu.service.impl.CommonMenuServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
