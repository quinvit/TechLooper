package com.techlooper.service.impl;

import com.techlooper.entity.CompanyEntity;
import com.techlooper.entity.CompanyJob;
import com.techlooper.entity.GitHubUserProfile;
import com.techlooper.entity.userimport.UserImportEntity;
import com.techlooper.model.SocialProvider;
import com.techlooper.repository.userimport.CompanyRepository;
import com.techlooper.service.CompanyService;
import org.dozer.Mapper;
import org.elasticsearch.index.query.FilterBuilders;
import org.elasticsearch.index.query.QueryBuilders;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.elasticsearch.core.ElasticsearchTemplate;
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Created by phuonghqh on 4/2/15.
 */
@Service
public class CompanyServiceImpl implements CompanyService {

  @Resource
  private ElasticsearchTemplate elasticsearchTemplateUserImport;

  @Resource
  private CompanyRepository companyRepository;

  @Value("${elasticsearch.userimport.index.name}")
  private String indexName;

  @Resource
  private Mapper dozerMapper;

  public CompanyEntity findById(Long id) {
    return companyRepository.findOne(id);
  }

  public CompanyEntity findByName(String companyName) {
    NativeSearchQueryBuilder queryBuilder = new NativeSearchQueryBuilder().withIndices(indexName)
      .withTypes("company");
    queryBuilder.withFilter(FilterBuilders.queryFilter(QueryBuilders.matchPhraseQuery("companyName", companyName)));

    CompanyEntity company = null;
    List<CompanyEntity> companies = elasticsearchTemplateUserImport.queryForList(queryBuilder.build(), CompanyEntity.class);
    if (companies.size() > 0) {
      company = companies.get(0);
      List<CompanyJob> topJobs = company.getJobs().stream().sorted((job1, job2) -> {
        if (job1.getExpiredDate() == null || job2.getExpiredDate() == null) {
          return -1;
        }
        return -1 * job1.getExpiredDate().compareTo(job2.getExpiredDate());
      }).collect(Collectors.toList());
      company.setJobs(topJobs);

      queryBuilder.withTypes("user").withFilter(FilterBuilders.nestedFilter("profiles", FilterBuilders.termFilter("profiles.GITHUB.company", companyName)));
      final CompanyEntity finalCompany = company;
      elasticsearchTemplateUserImport.queryForList(queryBuilder.build(), UserImportEntity.class).forEach(userEntity -> {
        Optional.ofNullable(userEntity.getProfiles().get(SocialProvider.GITHUB)).ifPresent(profile -> {
          GitHubUserProfile employee = new GitHubUserProfile();
          LinkedHashMap map = (LinkedHashMap) profile;
          employee.setEmail((String) map.get("email"));
          employee.setProfileImageUrl((String) map.get("imageUrl"));
          employee.setName((String) map.get("fullName"));
          employee.setUsername((String) map.get("username"));
          finalCompany.getEmployees().add(employee);
        });
      });
    }
    return company;
  }
}
