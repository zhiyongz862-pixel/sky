package com.sky.service.impl;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.sky.constant.MessageConstant;
import com.sky.constant.PasswordConstant;
import com.sky.constant.StatusConstant;
import com.sky.context.BaseContext;
import com.sky.dto.EmployeeDTO;
import com.sky.dto.EmployeeLoginDTO;
import com.sky.dto.EmployeePageQueryDTO;
import com.sky.entity.Employee;
import com.sky.exception.AccountLockedException;
import com.sky.exception.AccountNotFoundException;
import com.sky.exception.PasswordErrorException;
import com.sky.mapper.EmployeeMapper;
import com.sky.result.PageResult;
import com.sky.service.EmployeeService;
import lombok.Builder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

@Slf4j
@Service
//构建器，方便构建
@Builder
public class EmployeeServiceImpl implements EmployeeService {

    @Autowired
    private EmployeeMapper employeeMapper;

    /**
     * 员工登录
     *
     * @param employeeLoginDTO
     * @return
     */
    public Employee login(EmployeeLoginDTO employeeLoginDTO) {
        String username = employeeLoginDTO.getUsername();
        String password = employeeLoginDTO.getPassword();

        //1、根据用户名查询数据库中的数据
        Employee employee = employeeMapper.getByUsername(username);

        //2、处理各种异常情况（用户名不存在、密码不对、账号被锁定）
        if (employee == null) {
            //账号不存在
            throw new AccountNotFoundException(MessageConstant.ACCOUNT_NOT_FOUND);
        }

        //密码比对
        // 对前端传过来的明文进行MD5处理
        password = DigestUtils.md5DigestAsHex(employeeLoginDTO.getPassword().getBytes());
        if (!password.equals(employee.getPassword())) {
            //密码错误
            throw new PasswordErrorException(MessageConstant.PASSWORD_ERROR);
        }

        if (employee.getStatus() == StatusConstant.DISABLE) {
            //账号被锁定
            throw new AccountLockedException(MessageConstant.ACCOUNT_LOCKED);
        }

        //3、返回实体对象
        return employee;
    }

    @Override
    public void save(EmployeeDTO employeeDTO) {
        Employee employee = new Employee();
        // 使用BeanUtils工具类进行属性拷贝，将employeeDTO中的属性拷贝到employee中
        BeanUtils.copyProperties(employeeDTO, employee);
        employee.setStatus(StatusConstant.ENABLE);
        employee.setCreateTime(java.time.LocalDateTime.now());
        employee.setUpdateTime(java.time.LocalDateTime.now());
        //TODO 暂时获取不到，以后可以获取
        employee.setCreateUser(BaseContext.getCurrentId());
        employee.setUpdateUser(BaseContext.getCurrentId());
        // 设置密码，默认密码123456
        employee.setPassword(DigestUtils.md5DigestAsHex(PasswordConstant.DEFAULT_PASSWORD.getBytes()));
        // 保存员工信息
        employeeMapper.insert(employee);
    }

    @Override
    public PageResult page(EmployeePageQueryDTO employeePageQueryDTO) {
        PageHelper.startPage(employeePageQueryDTO.getPage(), employeePageQueryDTO.getPageSize());
        Page<Employee> page = employeeMapper.pageQuery(employeePageQueryDTO);
        return new PageResult(page.getTotal(), page.getResult());
    }

    @Override
    public void startOrStop(Integer status, Long id) {
    //    update employee set status = ? where id = ?
        Employee employee = Employee.builder()
                .status(status)
                .id(id)
                .build();
        employeeMapper.update(employee);

    }

    @Override
    public Employee getById(Long id) {
        return employeeMapper.getById(id);

    }

    @Override
    public void update(EmployeeDTO employeeDTO) {
        Employee employee = new Employee();
        BeanUtils.copyProperties(employeeDTO, employee);
        employee.setUpdateTime(java.time.LocalDateTime.now());
        employee.setUpdateUser(BaseContext.getCurrentId());
        employeeMapper.update(employee);
    }

}
